/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.context;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.wildfly.clustering.service.concurrent.Executor;
import org.wildfly.common.function.ExceptionRunnable;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * Facility for contextual execution.
 * @author Paul Ferraro
 */
public interface ContextualExecutor extends Contextualizer, Executor {

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
        return new ExceptionRunnable<E>() {
            @Override
            public void run() throws E {
                ContextualExecutor.this.execute(runner);
            }
        };
    }

    @Override
    default <T> Callable<T> contextualize(Callable<T> caller) {
        return new Callable<T>() {
            @Override
            public T call() throws Exception {
                return ContextualExecutor.this.execute(caller);
            }
        };
    }

    @Override
    default <T> Supplier<T> contextualize(Supplier<T> supplier) {
        return new Supplier<T>() {
            @Override
            public T get() {
                return ContextualExecutor.this.execute(supplier);
            }
        };
    }

    @Override
    default <T, E extends Exception> ExceptionSupplier<T, E> contextualize(ExceptionSupplier<T, E> supplier) {
        return new ExceptionSupplier<T, E>() {
            @Override
            public T get() throws E {
                return ContextualExecutor.this.execute(supplier);
            }
        };
    }
}
