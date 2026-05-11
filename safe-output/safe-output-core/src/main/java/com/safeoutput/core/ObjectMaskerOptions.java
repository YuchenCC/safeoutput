package com.safeoutput.core;

public final class ObjectMaskerOptions {

    private final int maxDepth;

    private final int maxCollectionSize;

    private ObjectMaskerOptions(Builder builder) {
        this.maxDepth = builder.maxDepth;
        this.maxCollectionSize = builder.maxCollectionSize;
    }

    public static ObjectMaskerOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getMaxCollectionSize() {
        return maxCollectionSize;
    }

    public static final class Builder {

        private int maxDepth = 8;

        private int maxCollectionSize = 1000;

        private Builder() {
        }

        public Builder maxDepth(int maxDepth) {
            this.maxDepth = Math.max(0, maxDepth);
            return this;
        }

        public Builder maxCollectionSize(int maxCollectionSize) {
            this.maxCollectionSize = Math.max(0, maxCollectionSize);
            return this;
        }

        public ObjectMaskerOptions build() {
            return new ObjectMaskerOptions(this);
        }
    }
}
