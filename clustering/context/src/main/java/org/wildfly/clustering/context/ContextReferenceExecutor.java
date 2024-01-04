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
 * Contextual executor based on a context reference.
 * @author Paul Ferraro
 */
public class ContextReferenceExecutor<C> implements ContextualExecutor {
    private final C targetContext;
    private final ContextReference<C> reference;

    public ContextReferenceExecutor(C targetContext, ContextReference<C> reference) {
        this.targetContext = targetContext;
        this.reference = reference;
    }

    @Override
    public void execute(Runnable runner) {
        C currentContext = this.reference.get();
        this.reference.accept(this.targetContext);
        try {
            runner.run();
        } finally {
            this.reference.accept(currentContext);
        }
    }

    @Override
    public <E extends Exception> void execute(ExceptionRunnable<E> runner) throws E {
        C currentContext = this.reference.get();
        this.reference.accept(this.targetContext);
        try {
            runner.run();
        } finally {
            this.reference.accept(currentContext);
        }
    }

    @Override
    public <T> T execute(Callable<T> caller) throws Exception {
        C currentContext = this.reference.get();
        this.reference.accept(this.targetContext);
        try {
            return caller.call();
        } finally {
            this.reference.accept(currentContext);
        }
    }

    @Override
    public <T> T execute(Supplier<T> supplier) {
        C currentContext = this.reference.get();
        this.reference.accept(this.targetContext);
        try {
            return supplier.get();
        } finally {
            this.reference.accept(currentContext);
        }
    }

    @Override
    public <T, E extends Exception> T execute(ExceptionSupplier<T, E> supplier) throws E {
        C currentContext = this.reference.get();
        this.reference.accept(this.targetContext);
        try {
            return supplier.get();
        } finally {
            this.reference.accept(currentContext);
        }
    }
}
