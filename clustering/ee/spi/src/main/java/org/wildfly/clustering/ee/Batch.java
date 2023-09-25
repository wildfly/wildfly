/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ee;

/**
 * Exposes a mechanism to close a batch, and potentially discard it.
 * @author Paul Ferraro
 */
public interface Batch extends AutoCloseable {

    /**
     * The possible states of a batch.
     */
    enum State {
        /**
         * The initial state of a batch.
         * A batch remains active until it is discarded or closed.
         */
        ACTIVE,
        /**
         * Indicates that an active batch was discarded, but not yet closed.
         * An active batch moves to this state following {@link Batch#discard()}.
         */
        DISCARDED,
        /**
         * The terminal state of a batch.
         * A batch moves to this state following {@link Batch#close()}.
         */
        CLOSED
    }

    /**
     * Closes this batch.  Batch may or may not have been discarded.
     */
    @Override
    void close();

    /**
     * Discards this batch.  A discarded batch must still be closed.
     */
    void discard();

    /**
     * Returns the state of this batch.
     * @return the state of this batch.
     */
    State getState();
}
