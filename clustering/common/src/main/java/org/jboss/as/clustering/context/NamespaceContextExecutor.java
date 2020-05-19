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

import org.jboss.as.naming.context.NamespaceContextSelector;
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
