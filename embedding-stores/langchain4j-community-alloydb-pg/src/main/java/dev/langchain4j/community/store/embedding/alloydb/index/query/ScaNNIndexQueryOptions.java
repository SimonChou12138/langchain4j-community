package dev.langchain4j.community.store.embedding.alloydb.index.query;

import java.util.List;

/**
 * ScaNN index query options
 */
public class ScaNNIndexQueryOptions implements QueryOptions {

    private final Integer numLeavesToSearch;
    private final Integer preOrderingNumNeighbors;

    /**
     * Constructor for ScaNNIndexQueryOptions
     *
     * @param builder builder
     */
    public ScaNNIndexQueryOptions(Builder builder) {
        this.numLeavesToSearch = builder.numLeavesToSearch;
        this.preOrderingNumNeighbors = builder.preOrderingNumNeighbors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getParameterSettings() {
        return List.of(
                "scann.num_leaves_to_search = " + numLeavesToSearch,
                "scann.pre_reordering_num_neighbors = " + preOrderingNumNeighbors);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder which configures and creates instances of {@link ScaNNIndexQueryOptions}.
     */
    public static class Builder {

        private Integer numLeavesToSearch = 1;
        private Integer preOrderingNumNeighbors = -1;

        /**
         * @param numLeavesToSearch number of probes
         * @return this builder
         */
        public Builder numLeavesToSearch(Integer numLeavesToSearch) {
            this.numLeavesToSearch = numLeavesToSearch;
            return this;
        }

        /**
         * @param preOrderingNumNeighbors number of preordering neighbors
         * @return this builder
         */
        public Builder preOrderingNumNeighbors(Integer preOrderingNumNeighbors) {
            this.preOrderingNumNeighbors = preOrderingNumNeighbors;
            return this;
        }

        /**
         * Builds an {@link ScaNNIndexQueryOptions} store with the configuration applied to this builder.
         *
         * @return A new {@link ScaNNIndexQueryOptions} instance
         */
        public ScaNNIndexQueryOptions build() {
            return new ScaNNIndexQueryOptions(this);
        }
    }
}
