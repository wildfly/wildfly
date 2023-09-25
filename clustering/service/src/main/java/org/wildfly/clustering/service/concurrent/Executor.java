/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service.concurrent;

import org.wildfly.common.function.ExceptionRunnable;

/**
 * Extends {@link java.util.concurrent.Executor} to additionally support a {@link ExceptonRunnable}.
 * @author Paul Ferraro
 * @deprecated To be removed without replacement
 */
@Deprecated(forRemoval = true)
public interface Executor extends java.util.concurrent.Executor {

    /**
     * Executes the specified runner.
     * @param <E> the exception type
     * @param runner a runnable task
     * @throws E if execution fails
     */
    <E extends Exception> void execute(ExceptionRunnable<E> runner) throws E;
}
