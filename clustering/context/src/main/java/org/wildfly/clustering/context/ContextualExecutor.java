/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.context;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.wildfly.common.function.ExceptionRunnable;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * Facility for contextual execution.
 * @author Paul Ferraro
 */
public interface ContextualExecutor extends Contextualizer, Executor {

    /**
     * Executes the specified runner.
     * @param <E> the exception type
     * @param runner a runnable task
     * @throws E if execution fails
     */
    <E extends Exception> void execute(ExceptionRunnable<E> runner) throws E;

    /**
     * Executes the specified caller with a given context.
     * @param <T> the return type
     * @param caller a callable task
     * @return the result of the caller
     * @throws Exception if execution fails
     */
    <T> T execute(Callable<T> caller) throws Exception;

    /**
     * Executes the specified supplier with a given context.
     * @param <T> the return type
     * @param supplier a supplier task
     * @return the result of the supplier
     */
    <T> T execute(Supplier<T> supplier);

    /**
     * Executes the specified supplier with a given context.
     * @param <T> the return type
     * @param <E> the exception type
     * @param supplier a supplier task
     * @return the result of the supplier
     * @throws E if execution fails
     */
    <T, E extends Exception> T execute(ExceptionSupplier<T, E> supplier) throws E;

    @Override
    default Runnable contextualize(Runnable runner) {
        return new Runnable() {
            @Override
            public void run() {
                ContextualExecutor.this.execute(runner);
            }
        };
    }

    @Override
    default <E extends Exception> ExceptionRunnable<E> contextualize(ExceptionRunnable<E> runner) {
        return new ExceptionRunnable<>() {
            @Override
            public void run() throws E {
                ContextualExecutor.this.execute(runner);
            }
        };
    }

    @Override
    default <T> Callable<T> contextualize(Callable<T> caller) {
        return new Callable<>() {
            @Override
            public T call() throws Exception {
                return ContextualExecutor.this.execute(caller);
            }
        };
    }

    @Override
    default <T> Supplier<T> contextualize(Supplier<T> supplier) {
        return new Supplier<>() {
            @Override
            public T get() {
                return ContextualExecutor.this.execute(supplier);
            }
        };
    }

    @Override
    default <T, E extends Exception> ExceptionSupplier<T, E> contextualize(ExceptionSupplier<T, E> supplier) {
        return new ExceptionSupplier<>() {
            @Override
            public T get() throws E {
                return ContextualExecutor.this.execute(supplier);
            }
        };
    }
}
