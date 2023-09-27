/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.context;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.wildfly.common.function.ExceptionRunnable;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * Facility for creating contextual tasks.
 * @author Paul Ferraro
 */
public interface Contextualizer {

    /**
     * Decorates the specified runner with a given context.
     * @param runner a runnable task
     * @return a contextual runner
     */
    Runnable contextualize(Runnable runner);

    /**
     * Decorates the specified runner with a given context.
     * @param <E> the exception type
     * @param runner a runnable task
     * @return a contextual runner
     */
    <E extends Exception> ExceptionRunnable<E> contextualize(ExceptionRunnable<E> runner);

    /**
     * Decorates the specified caller with a given context.
     * @param <T> the return type
     * @param runner a callable task
     * @return a contextual caller
     */
    <T> Callable<T> contextualize(Callable<T> caller);

    /**
     * Decorates the specified supplier with a given context.
     * @param <T> the return type
     * @param runner a supplier task
     * @return a contextual supplier
     */
    <T> Supplier<T> contextualize(Supplier<T> supplier);

    /**
     * Decorates the specified supplier with a given context.
     * @param <T> the return type
     * @param <E> the exception type
     * @param runner a supplier task
     * @return a contextual supplier
     */
    <T, E extends Exception> ExceptionSupplier<T, E> contextualize(ExceptionSupplier<T, E> supplier);
}
