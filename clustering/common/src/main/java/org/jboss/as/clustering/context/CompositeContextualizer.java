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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.wildfly.common.function.ExceptionRunnable;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * A {@link Contextualizer} decorator that creates contextual tasks from a multiple contextualizers.
 * @author Paul Ferraro
 */
public class CompositeContextualizer implements Contextualizer {

    private final List<Contextualizer> contextualizers;

    public CompositeContextualizer(Contextualizer... contextualizers) {
        this(Arrays.asList(contextualizers));
    }

    public CompositeContextualizer(List<Contextualizer> contextualizers) {
        this.contextualizers = contextualizers;
    }

    @Override
    public Runnable contextualize(Runnable runner) {
        Runnable result = runner;
        for (Contextualizer context : this.contextualizers) {
            result = context.contextualize(result);
        }
        return result;
    }

    @Override
    public <E extends Exception> ExceptionRunnable<E> contextualize(ExceptionRunnable<E> runner) {
        ExceptionRunnable<E> result = runner;
        for (Contextualizer context : this.contextualizers) {
            result = context.contextualize(result);
        }
        return result;
    }

    @Override
    public <T> Callable<T> contextualize(Callable<T> caller) {
        Callable<T> result = caller;
        for (Contextualizer context : this.contextualizers) {
            result = context.contextualize(result);
        }
        return result;
    }

    @Override
    public <T> Supplier<T> contextualize(Supplier<T> supplier) {
        Supplier<T> result = supplier;
        for (Contextualizer context : this.contextualizers) {
            result = context.contextualize(result);
        }
        return result;
    }

    @Override
    public <T, E extends Exception> ExceptionSupplier<T, E> contextualize(ExceptionSupplier<T, E> supplier) {
        ExceptionSupplier<T, E> result = supplier;
        for (Contextualizer context : this.contextualizers) {
            result = context.contextualize(result);
        }
        return result;
    }
}
