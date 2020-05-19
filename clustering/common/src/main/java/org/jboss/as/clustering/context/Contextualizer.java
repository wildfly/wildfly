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
