/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee;

import java.util.stream.Stream;

/**
 * A task scheduler.
 * @author Paul Ferraro
 */
public interface Scheduler<I, M> extends AutoCloseable {
    /**
     * Schedules a task for the object with the specified identifier, using the specified metaData
     * @param id an object identifier
     * @param metaData the object meta-data
     */
    void schedule(I id, M metaData);

    /**
     * Cancels a previously scheduled task for the object with the specified identifier.
     * @param id an object identifier
     */
    void cancel(I id);

    /**
     * Returns a stream of scheduled item identifiers.
     * @return a stream of scheduled item identifiers.
     */
    Stream<I> stream();

    /**
     * Indicates whether the object with the specified identifier is scheduled.
     * @param id an object identifier
     */
    default boolean contains(I id) {
        return this.stream().anyMatch(id::equals);
    }

    /**
     * Closes any resources used by this scheduler.
     */
    @Override
    void close();
}
