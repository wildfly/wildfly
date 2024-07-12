/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * Hack to take a task and run it on a virtual thread while
 * the caller thread that would normally run the task itself
 * blocks until the virtual thread completes. The point of this
 * is just to get work running on virtual threads so monitoring
 * tools can monitor their behavior when run that way.
 */
final class VirtualThreadDispatch {

    private static final Method startMethod;

    static {
        Method method = null;

        try {
            Class<?> clazz = Class.forName("java.lang.Thread$Builder");
            //noinspection JavaReflectionMemberAccess
            method = Thread.class.getMethod("startVirtualThread", Runnable.class);
        } catch (ReflectiveOperationException e) {
            // Virtual threads are not supported
        }
        startMethod = method;
    }

    static boolean canRunVirtual() {
        return startMethod != null;
    }

    static void runVirtual(Runnable r) {
        if (canRunVirtual()) {
            WrapperRunnable<Void> runnable = new WrapperRunnable<>(r);
            executeOnVirtualThread(runnable);
            Throwable t = runnable.thrownValue.get();
            if (t != null) {
                rethrowUnchecked(t);
            }
        } else {
            r.run();
        }
    }

    static <V> V runVirtual(Callable<V> c) throws Exception {
        if (canRunVirtual()) {
            WrapperRunnable<V> runnable = new WrapperRunnable<>(c);
            executeOnVirtualThread(runnable);
            Throwable t = runnable.thrownValue.get();
            if (t != null) {
                rethrowChecked(t);
                throw new IllegalStateException("unreachable");
            } else {
                return runnable.returnValue.get();
            }
        } else {
            return c.call();
        }
    }

    private static void executeOnVirtualThread(WrapperRunnable<?> r) {
        Thread t = null;
        try {
            t = (Thread) startMethod.invoke(null, r);
            t.join();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            t.interrupt();
            throw new RuntimeException(e);
        }
    }

    private static void rethrowUnchecked(Throwable t) {
        try {
            rethrowChecked(t);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Exception illegal) {
            // This should not be called from code that can catch a checked exception
            throw new IllegalStateException(illegal.getMessage(), illegal);
        }
    }

    private static void rethrowChecked(Throwable t) throws Exception {
        Throwable wrapped = getTwoParamWrapper(t);
        if (wrapped == null) {
            wrapped = getSingleParamWrapper(t);
            if (wrapped == null) {
                wrapped = getBasicWrapper(t);
            }
        }
        if (wrapped instanceof RuntimeException) {
            throw (RuntimeException) wrapped;
        } else if (wrapped instanceof Exception) {
            throw (Exception) wrapped;
        } else if (wrapped instanceof Error) {
            throw (Error) wrapped;
        } else {
            // This should not be called from code that can catch a Throwable that isn't an Exception or Error
            throw new IllegalStateException(wrapped.getMessage(), wrapped);
        }
    }

    private static Throwable getTwoParamWrapper(Throwable t) {
        Class<? extends Throwable> clazz = t.getClass();
        Throwable result = null;
        try {
            Constructor<? extends Throwable> ctor = clazz.getConstructor(String.class, Throwable.class);
            result = ctor.newInstance(t.getMessage(), t);
        } catch (Exception e) {
            // fall through
        }
        return result;
    }

    private static Throwable getSingleParamWrapper(Throwable t) {
        Class<? extends Throwable> clazz = t.getClass();
        Throwable result = null;
        try {
            Constructor<? extends Throwable> ctor = clazz.getConstructor(String.class);
            Throwable newT = ctor.newInstance(t.getMessage());
            newT.initCause(t);
            result = newT;
        } catch (Exception e) {
            // fall through
        }
        return result;
    }

    private static Throwable getBasicWrapper(Throwable t) {
        if (t instanceof RuntimeException) {
            return new RuntimeException(t.getMessage(), t);
        } else if (t instanceof Exception) {
            return new Exception(t.getMessage(), t);
        } else if (t instanceof Error) {
            return new Error(t.getMessage(), t);
        } else {
            return new Throwable(t.getMessage(), t);
        }
    }

    static class VirtualThreadHandler implements HttpHandler {

        private final HttpHandler wrapped;

        private VirtualThreadHandler(HttpHandler wrapped) {
            this.wrapped = wrapped;
        }
        @Override
        public void handleRequest(final HttpServerExchange exchange) throws Exception {
            VirtualThreadDispatch.runVirtual(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    wrapped.handleRequest(exchange);
                    return null;
                }
            });
        }

        static class Wrapper implements HandlerWrapper {

            @Override
            public HttpHandler wrap(HttpHandler handler) {
                if (canRunVirtual()) {
                    return new VirtualThreadHandler(handler);
                } else {
                    return handler;
                }
            }
        }
    }

    private static class WrapperRunnable<V> implements Runnable {

        private final Callable<V> callable;
        private final Runnable runnable;
        private final AtomicReference<V> returnValue;
        private final AtomicReference<Throwable> thrownValue;

        private WrapperRunnable(Callable<V> callable) {
            this.callable = callable;
            this.runnable = null;
            this.returnValue = new AtomicReference<>();
            this.thrownValue = new AtomicReference<>();
        }

        private WrapperRunnable(Runnable runnable) {
            this.callable = null;
            this.runnable = runnable;
            this.returnValue = null;
            this.thrownValue = new AtomicReference<>();
        }

        public void run() {
            if (runnable != null) {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    thrownValue.set(t);
                }
            } else if (callable != null) {
                try {
                    assert returnValue != null;
                    returnValue.set(callable.call());
                } catch (Throwable t) {
                    thrownValue.set(t);
                }
            }
        }
    }
}
