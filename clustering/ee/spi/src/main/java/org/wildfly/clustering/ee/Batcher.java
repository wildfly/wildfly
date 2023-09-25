/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ee;

/**
 * Exposes a mechanism to handle batching.
 * @author Paul Ferraro
 */
public interface Batcher<B extends Batch> {
    /**
     * Creates a batch.
     * @return a batch.
     */
    B createBatch();

    /**
     * Resumes a batch. Used if the specified batch was (or may have been) created by another thread.
     * @param batch an existing batch
     * @return the context of the resumed batch
     */
    BatchContext resumeBatch(B batch);

    /**
     * Suspends a batch.
     * @return the previously active batch, or null if there was no active batch
     */
    B suspendBatch();
}
