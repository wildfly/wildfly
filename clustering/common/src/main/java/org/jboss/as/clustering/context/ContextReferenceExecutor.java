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
