package dev.langchain4j.community.store.embedding.alloydb.index;

import java.util.List;

/**
 * ScaNN index
 */
public class ScaNNIndex implements BaseIndex {

    private final String indexType = "ScaNN";
    private final String name;
    private final Integer numLeaves;
    private final String quantizer;
    private final DistanceStrategy distanceStrategy;
    private final List<String> partialIndexes;

    /**
     * Constructor for ScaNNIndex
     *
     * @param builder builder
     */
    public ScaNNIndex(Builder builder) {
        this.name = builder.name;
        this.numLeaves = builder.numLeaves;
        this.quantizer = builder.quantizer;
        this.distanceStrategy = builder.distanceStrategy;
        this.partialIndexes = builder.partialIndexes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIndexOptions() {
        return String.format("(num_leaves = %s, quantizer = %s)", numLeaves, quantizer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIndexType() {
        return indexType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DistanceStrategy getDistanceStrategy() {
        return distanceStrategy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getPartialIndexes() {
        return partialIndexes;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder which configures and creates instances of {@link ScaNNIndex}.
     */
    public static class Builder {

        private String name;
        private Integer numLeaves = 5;
        private String quantizer = "sq8";
        private DistanceStrategy distanceStrategy = DistanceStrategy.COSINE_DISTANCE;
        private List<String> partialIndexes;

        /**
         * @param name name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * @param numLeaves numLeaves
         * @return this builder
         */
        public Builder numLeaves(Integer numLeaves) {
            this.numLeaves = numLeaves;
            return this;
        }

        /**
         * @param quantizer quantizer
         * @return thisbuilder
         */
        public Builder quantizer(String quantizer) {
            this.quantizer = quantizer;
            return this;
        }

        /**
         * @param distanceStrategy distance strategy
         * @return thisbuilder
         */
        public Builder distanceStrategy(DistanceStrategy distanceStrategy) {
            this.distanceStrategy = distanceStrategy;
            return this;
        }

        /**
         * @param partialIndexes partial indexes
         * @return thisbuilder
         */
        public Builder partialIndexes(List<String> partialIndexes) {
            this.partialIndexes = partialIndexes;
            return this;
        }

        /**
         * Builds an {@link ScaNNIndex} store with the configuration applied to this builder.
         *
         * @return A new {@link ScaNNIndex} instance
         */
        public ScaNNIndex build() {
            return new ScaNNIndex(this);
        }
    }
}
