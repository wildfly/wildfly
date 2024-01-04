/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

/**
 * Set of factory implementations for creating an {@link ExecutorService} from a {@link ThreadFactory}.
 * @author Paul Ferraro
 */
public enum ExecutorServiceFactory implements Function<ThreadFactory, ExecutorService> {

    SINGLE_THREAD() {
        @Override
        public ExecutorService apply(ThreadFactory factory) {
            return Executors.newSingleThreadExecutor(factory);
        }
    },
    CACHED_THREAD() {
        @Override
        public ExecutorService apply(ThreadFactory factory) {
            return Executors.newCachedThreadPool(factory);
        }
    },
    ;
}
