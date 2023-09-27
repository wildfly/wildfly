/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.context;

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

        ExceptionRunnable<Exception> exceptionRunner = new ExceptionRunnable<>() {
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

        Callable<Object> caller = new Callable<>() {
            @Override
            public Object call() {
                assertSame(target, contextRef.get());
                return result;
            }
        };

        assertSame(original, contextRef.get());
        assertSame(result, contextualizer.contextualize(caller).call());
        assertSame(original, contextRef.get());

        Supplier<Object> supplier = new Supplier<>() {
            @Override
            public Object get() {
                assertSame(target, contextRef.get());
                return result;
            }
        };

        assertSame(original, contextRef.get());
        assertSame(result, contextualizer.contextualize(supplier).get());
        assertSame(original, contextRef.get());

        ExceptionSupplier<Object, Exception> exceptionSupplier = new ExceptionSupplier<>() {
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
