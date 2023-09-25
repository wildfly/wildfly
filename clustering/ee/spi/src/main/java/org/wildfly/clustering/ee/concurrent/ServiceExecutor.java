/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.concurrent;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.wildfly.common.function.ExceptionRunnable;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * Allows safe invocation of tasks that require resources not available after {@link #close(Runnable)} to block a service from stopping.
 * @author Paul Ferraro
 */
public interface ServiceExecutor extends Executor {

    /**
     * Executes the specified runner.
     * @param <E> the exception type
     * @param runner a runnable task
     * @throws E if execution fails
     */
    <E extends Exception> void execute(ExceptionRunnable<E> runner) throws E;

    /**
     * Executes the specified task, but only if the service was not already closed.
     * If service is already closed, the task is not run.
     * If executed, the specified task must return a non-null value, to be distinguishable from a non-execution.
     * @param executeTask a task to execute
     * @return an optional value that is present only if the specified task was run.
     */
    <R> Optional<R> execute(Supplier<R> executeTask);

    /**
     * Executes the specified task, but only if the service was not already closed.
     * If service is already closed, the task is not run.
     * If executed, the specified task must return a non-null value, to be distinguishable from a non-execution.
     * @param executeTask a task to execute
     * @return an optional value that is present only if the specified task was run.
     * @throws E if the task execution failed
     */
    <R, E extends Exception> Optional<R> execute(ExceptionSupplier<R, E> executeTask) throws E;

    /**
     * Closes the service, executing the specified task, first waiting for any concurrent executions to complete.
     * The specified task will only execute once, irrespective on subsequent {@link #close(Runnable)} invocations.
     * @param closeTask a task which closes the service
     */
    void close(Runnable closeTask);
}
