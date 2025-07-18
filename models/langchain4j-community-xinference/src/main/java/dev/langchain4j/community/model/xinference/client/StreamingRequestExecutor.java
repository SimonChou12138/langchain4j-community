package dev.langchain4j.community.model.xinference.client;

import static dev.langchain4j.community.model.xinference.client.utils.ExceptionUtil.toException;
import static dev.langchain4j.community.model.xinference.client.utils.JsonUtil.toJson;
import static dev.langchain4j.community.model.xinference.client.utils.JsonUtil.toObject;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StreamingRequestExecutor<Request, Response, ResponseContent> {
    private static final Logger log = LoggerFactory.getLogger(StreamingRequestExecutor.class);
    private final OkHttpClient okHttpClient;
    private final String endpointUrl;
    private final Supplier<Request> requestWithStreamSupplier;
    private final Class<Response> responseClass;
    private final Function<Response, ResponseContent> streamEventContentExtractor;
    private final boolean logStreamingResponses;
    private final ResponseLoggingInterceptor responseLogger = new ResponseLoggingInterceptor();

    StreamingRequestExecutor(
            OkHttpClient okHttpClient,
            String endpointUrl,
            Supplier<Request> requestWithStreamSupplier,
            Class<Response> responseClass,
            Function<Response, ResponseContent> streamEventContentExtractor,
            boolean logStreamingResponses) {
        this.okHttpClient = okHttpClient;
        this.endpointUrl = endpointUrl;
        this.requestWithStreamSupplier = requestWithStreamSupplier;
        this.responseClass = responseClass;
        this.streamEventContentExtractor = streamEventContentExtractor;
        this.logStreamingResponses = logStreamingResponses;
    }

    StreamingResponseHandling onPartialResponse(Consumer<ResponseContent> partialResponseHandler) {
        return new StreamingResponseHandling() {
            @Override
            public StreamingCompletionHandling onComplete(Runnable streamingCompletionCallback) {
                return new StreamingCompletionHandling() {
                    @Override
                    public ErrorHandling onError(Consumer<Throwable> errorHandler) {
                        return () -> stream(partialResponseHandler, streamingCompletionCallback, errorHandler);
                    }

                    @Override
                    public ErrorHandling ignoreErrors() {
                        return () -> stream(partialResponseHandler, streamingCompletionCallback, (e) -> {
                            // intentionally ignoring because user called ignoreErrors()
                        });
                    }
                };
            }

            @Override
            public ErrorHandling onError(Consumer<Throwable> errorHandler) {
                return () -> stream(
                        partialResponseHandler,
                        () -> {
                            // intentionally ignoring because user did not provide callback
                        },
                        errorHandler);
            }

            @Override
            public ErrorHandling ignoreErrors() {
                return () -> stream(
                        partialResponseHandler,
                        () -> {
                            // intentionally ignoring because user did not provide callback
                        },
                        (e) -> {
                            // intentionally ignoring because user called ignoreErrors()
                        });
            }
        };
    }

    private ResponseHandle stream(
            Consumer<ResponseContent> partialResponseHandler,
            Runnable streamingCompletionCallback,
            Consumer<Throwable> errorHandler) {

        Request request = requestWithStreamSupplier.get();

        String requestJson = toJson(request);

        okhttp3.Request okHttpRequest = new okhttp3.Request.Builder()
                .url(endpointUrl)
                .post(RequestBody.create(requestJson, MediaType.get("application/json; charset=utf-8")))
                .build();

        ResponseHandle responseHandle = new ResponseHandle();

        EventSourceListener eventSourceListener = new EventSourceListener() {

            @Override
            public void onOpen(@NotNull EventSource eventSource, @NotNull okhttp3.Response response) {
                if (responseHandle.cancelled) {
                    eventSource.cancel();
                    return;
                }

                if (logStreamingResponses) {
                    responseLogger.log(response);
                }
            }

            @Override
            public void onEvent(@NotNull EventSource eventSource, String id, String type, @NotNull String data) {
                if (responseHandle.cancelled) {
                    eventSource.cancel();
                    return;
                }

                if (logStreamingResponses) {
                    responseLogger.log("onEvent() {}", data);
                }

                if ("[DONE]".equals(data)) {
                    return;
                }

                try {
                    Response response = toObject(data, responseClass);
                    ResponseContent responseContent = streamEventContentExtractor.apply(response);
                    if (responseContent != null) {
                        partialResponseHandler.accept(responseContent); // do not handle exception, fail-fast
                    }
                } catch (Exception e) {
                    errorHandler.accept(e);
                }
            }

            @Override
            public void onClosed(@NotNull EventSource eventSource) {
                if (responseHandle.cancelled) {
                    eventSource.cancel();
                    return;
                }
                if (logStreamingResponses) {
                    responseLogger.log("onClosed()");
                }
                streamingCompletionCallback.run();
            }

            @Override
            public void onFailure(@NotNull EventSource eventSource, Throwable t, okhttp3.Response response) {
                if (responseHandle.cancelled) {
                    return;
                }

                // TODO remove this when migrating from okhttp
                if (t instanceof IllegalArgumentException && "byteCount < 0: -1".equals(t.getMessage())) {
                    streamingCompletionCallback.run();
                    return;
                }

                if (logStreamingResponses) {
                    responseLogger.log("onFailure()", t);
                    responseLogger.log(response);
                }

                if (t != null) {
                    errorHandler.accept(t); // TODO also include information from response?
                } else {
                    try {
                        errorHandler.accept(toException(response));
                    } catch (IOException e) {
                        errorHandler.accept(e); // TODO right thing to do?
                    }
                }
            }
        };
        EventSources.createFactory(okHttpClient).newEventSource(okHttpRequest, eventSourceListener);
        return responseHandle;
    }
}
