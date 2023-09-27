/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.function;

import java.util.function.Consumer;

import org.jboss.as.clustering.logging.ClusteringLogger;

/**
 * {@link Consumer} utility methods.
 * @author Paul Ferraro
 */
public class Consumers {

    /**
     * Returns a consumer that closes its input.
     * @return a consumer that closes its input.
     */
    public static <T extends AutoCloseable> Consumer<T> close() {
        return value -> {
            try {
                value.close();
            } catch (Throwable e) {
                ClusteringLogger.ROOT_LOGGER.failedToClose(e, value);
            }
        };
    }

    private Consumers() {
        // Hide
    }
}
