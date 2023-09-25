/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.naming;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.jboss.as.naming.context.NamespaceContextSelector;
import org.wildfly.clustering.context.ContextualExecutor;
import org.wildfly.common.function.ExceptionRunnable;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * A contextual executor that applies namespace context that was active when this object was constructed.
 * @author Paul Ferraro
 */
public class NamespaceContextExecutor implements ContextualExecutor {

    private final NamespaceContextSelector selector = NamespaceContextSelector.getCurrentSelector();

    @Override
    public void execute(Runnable runner) {
        if (this.selector != null) {
            NamespaceContextSelector.pushCurrentSelector(this.selector);
        }
        try {
            runner.run();
        } finally {
            if (this.selector != null) {
                NamespaceContextSelector.popCurrentSelector();
            }
        }
    }

    @Override
    public <E extends Exception> void execute(ExceptionRunnable<E> runner) throws E {
        if (this.selector != null) {
            NamespaceContextSelector.pushCurrentSelector(this.selector);
        }
        try {
            runner.run();
        } finally {
            if (this.selector != null) {
                NamespaceContextSelector.popCurrentSelector();
            }
        }
    }

    @Override
    public <T> T execute(Callable<T> caller) throws Exception {
        if (this.selector != null) {
            NamespaceContextSelector.pushCurrentSelector(this.selector);
        }
        try {
            return caller.call();
        } finally {
            if (this.selector != null) {
                NamespaceContextSelector.popCurrentSelector();
            }
        }
    }

    @Override
    public <T> T execute(Supplier<T> supplier) {
        if (this.selector != null) {
            NamespaceContextSelector.pushCurrentSelector(this.selector);
        }
        try {
            return supplier.get();
        } finally {
            if (this.selector != null) {
                NamespaceContextSelector.popCurrentSelector();
            }
        }
    }

    @Override
    public <T, E extends Exception> T execute(ExceptionSupplier<T, E> supplier) throws E {
        if (this.selector != null) {
            NamespaceContextSelector.pushCurrentSelector(this.selector);
        }
        try {
            return supplier.get();
        } finally {
            if (this.selector != null) {
                NamespaceContextSelector.popCurrentSelector();
            }
        }
    }

    @Override
    public Runnable contextualize(Runnable runner) {
        return (this.selector != null) ? ContextualExecutor.super.contextualize(runner) : runner;
    }

    @Override
    public <E extends Exception> ExceptionRunnable<E> contextualize(ExceptionRunnable<E> runner) {
        return (this.selector != null) ? ContextualExecutor.super.contextualize(runner) : runner;
    }

    @Override
    public <T> Callable<T> contextualize(Callable<T> caller) {
        return (this.selector != null) ? ContextualExecutor.super.contextualize(caller) : caller;
    }

    @Override
    public <T> Supplier<T> contextualize(Supplier<T> supplier) {
        return (this.selector != null) ? ContextualExecutor.super.contextualize(supplier) : supplier;
    }

    @Override
    public <T, E extends Exception> ExceptionSupplier<T, E> contextualize(ExceptionSupplier<T, E> supplier) {
        return (this.selector != null) ? ContextualExecutor.super.contextualize(supplier) : supplier;
    }
}
