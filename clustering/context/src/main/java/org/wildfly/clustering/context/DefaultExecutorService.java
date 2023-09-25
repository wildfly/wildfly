/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

import org.wildfly.security.ParametricPrivilegedAction;

/**
 * {@link ExecutorService} that performs contextual execution of submitted tasks.
 * @author Paul Ferraro
 */
public class DefaultExecutorService extends ContextualExecutorService {

    public static final ParametricPrivilegedAction<Void, ExecutorService> SHUTDOWN_ACTION = new ParametricPrivilegedAction<>() {
        @Override
        public Void run(ExecutorService executor) {
            executor.shutdown();
            return null;
        }
    };

    public static final ParametricPrivilegedAction<List<Runnable>, ExecutorService> SHUTDOWN_NOW_ACTION = new ParametricPrivilegedAction<>() {
        @Override
        public List<Runnable> run(ExecutorService executor) {
            return executor.shutdownNow();
        }
    };

    public DefaultExecutorService(Class<?> targetClass, Function<ThreadFactory, ExecutorService> factory) {
        // Use thread group of current thread
        super(factory.apply(new DefaultThreadFactory(targetClass, Thread.currentThread()::getThreadGroup)), DefaultContextualizerFactory.INSTANCE.createContextualizer(targetClass));
    }
}
