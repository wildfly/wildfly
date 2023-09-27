/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.context;

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
