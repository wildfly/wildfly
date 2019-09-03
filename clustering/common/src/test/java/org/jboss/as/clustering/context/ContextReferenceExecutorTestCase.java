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

import static org.junit.Assert.*;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.Test;
import org.wildfly.common.function.ExceptionRunnable;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * Unit test for {@link ContextReferenceExecutor}.
 * @author Paul Ferraro
 */
public class ContextReferenceExecutorTestCase {

    @Test
    public void test() throws Exception {
        Object original = new Object();
        Object target = new Object();
        Object result = new Object();
        AtomicReference<Object> resultRef = new AtomicReference<>();
        ContextReference<Object> contextRef = new AtomicContextReference<>(original);
        Contextualizer contextualizer = new ContextReferenceExecutor<>(target, contextRef);

        Runnable runner = new Runnable() {
            @Override
            public void run() {
                assertSame(target, contextRef.get());
                resultRef.set(result);
            }
        };

        assertSame(original, contextRef.get());
        contextualizer.contextualize(runner).run();
        assertSame(original, contextRef.get());

        assertSame(result, resultRef.get());
        resultRef.set(null);

        ExceptionRunnable<Exception> exceptionRunner = new ExceptionRunnable<Exception>() {
            @Override
            public void run() throws Exception {
                assertSame(target, contextRef.get());
                resultRef.set(result);
            }
        };

        assertSame(original, contextRef.get());
        contextualizer.contextualize(exceptionRunner).run();
        assertSame(original, contextRef.get());

        assertSame(result, resultRef.get());
        resultRef.set(null);

        Callable<Object> caller = new Callable<Object>() {
            @Override
            public Object call() {
                assertSame(target, contextRef.get());
                return result;
            }
        };

        assertSame(original, contextRef.get());
        assertSame(result, contextualizer.contextualize(caller).call());
        assertSame(original, contextRef.get());

        Supplier<Object> supplier = new Supplier<Object>() {
            @Override
            public Object get() {
                assertSame(target, contextRef.get());
                return result;
            }
        };

        assertSame(original, contextRef.get());
        assertSame(result, contextualizer.contextualize(supplier).get());
        assertSame(original, contextRef.get());

        ExceptionSupplier<Object, Exception> exceptionSupplier = new ExceptionSupplier<Object, Exception>() {
            @Override
            public Object get() {
                assertSame(target, contextRef.get());
                return result;
            }
        };

        assertSame(original, contextRef.get());
        assertSame(result, contextualizer.contextualize(exceptionSupplier).get());
        assertSame(original, contextRef.get());
    }

    static class AtomicContextReference<T> implements ContextReference<T> {
        private AtomicReference<T> ref;

        AtomicContextReference(T initial) {
            this.ref = new AtomicReference<>(initial);
        }

        @Override
        public void accept(T value) {
            this.ref.set(value);
        }

        @Override
        public T get() {
            return this.ref.get();
        }
    }
}
