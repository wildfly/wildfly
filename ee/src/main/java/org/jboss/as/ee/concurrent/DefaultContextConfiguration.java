/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.concurrent;

import org.jboss.invocation.InterceptorContext;
import org.wildfly.ee.concurrent.ContextConfiguration;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.enterprise.concurrent.Trigger;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * The implementation for the default context configuration to be used in the default managed objects. By the spec, the default managed objects must - at least - use the security, naming and classloading contexts of the creator thread.
 *
 * @author Eduardo Martins
 */
public class DefaultContextConfiguration implements ContextConfiguration {

    private static final Object[] NO_PARAMS = {};

    private static Method getDeclaredMethod(final Class<?> type, final String methodName, final Class<?>... parameterTypes) {
        final SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            return AccessController.doPrivileged(new PrivilegedAction<Method>() {
                @Override
                public Method run() {
                    try {
                        return type.getDeclaredMethod(methodName, parameterTypes);
                    } catch (NoSuchMethodException e) {
                        return null;
                    }
                }
            });
        } else {
            try {
                return type.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                return null;
            }
        }
    }

    @Override
    public <V> Callable<V> newContextualCallable(final Callable<V> callable) {
        return new ContextualCallable<>(getConcurrentContext().getDefaultInterceptorContext(), callable);
    }

    @Override
    public Runnable newContextualRunnable(Runnable runnable) {
        return new ContextualRunnable(getConcurrentContext().getDefaultInterceptorContext(), runnable);
    }

    @Override
    public ManagedTaskListener newContextualManagedTaskListener(ManagedTaskListener listener) {
        return new ContextualManagedTaskListener(getConcurrentContext().getDefaultInterceptorContext(), listener);
    }

    @Override
    public InvocationHandler newContextualInvocationHandler(Object instance) {
        return new ContextualInvocationHandler(getConcurrentContext().getDefaultInterceptorContext(), instance);
    }

    @Override
    public Trigger newContextualTrigger(Trigger trigger) {
        return new ContextualTrigger(getConcurrentContext().getDefaultInterceptorContext(), trigger);
    }

    // contextual wrapping types which use an interceptor context to delegate

    @Override
    public Runnable newManageableThreadContextualRunnable(Runnable runnable) {
        return new ContextualRunnable(getConcurrentContext().getDefaultManagedThreadFactoryInterceptorContext(), runnable);
    }

    private ConcurrentContext getConcurrentContext() throws IllegalStateException {
        final ConcurrentContext concurrentContext = ConcurrentContext.current();
        if (concurrentContext == null) {
            throw new IllegalStateException("no concurrent context currently set, unable to retrieve the context interceptor");
        }
        return concurrentContext;
    }

    abstract static class ContextualObject {

        private final InterceptorContext interceptorContext;
        private final Object target;

        ContextualObject(InterceptorContext interceptorContext, Object target) {
            this.interceptorContext = interceptorContext;
            this.target = target;
        }

        Object interceptorContextProceed(Method method, Object[] params) throws Exception {
            final InterceptorContext clone = interceptorContext.clone();
            clone.setTarget(target);
            clone.setMethod(method);
            clone.setParameters(params);
            return clone.proceed();
        }
    }

    static class ContextualCallable<V> extends ContextualObject implements Callable<V> {

        private static final Method CALL_METHOD = getDeclaredMethod(Callable.class, "call");

        ContextualCallable(InterceptorContext interceptorContext, Callable<V> target) {
            super(interceptorContext, target);
        }

        @Override
        public V call() throws Exception {
            return (V) interceptorContextProceed(CALL_METHOD, NO_PARAMS);
        }
    }

    static class ContextualRunnable extends ContextualObject implements Runnable {

        private static final Method RUN_METHOD = getDeclaredMethod(Runnable.class, "run");

        ContextualRunnable(InterceptorContext interceptorContext, Runnable target) {
            super(interceptorContext, target);
        }

        @Override
        public void run() {
            try {
                interceptorContextProceed(RUN_METHOD, NO_PARAMS);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class ContextualInvocationHandler extends ContextualObject implements InvocationHandler {

        ContextualInvocationHandler(InterceptorContext interceptorContext, Object target) {
            super(interceptorContext, target);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return interceptorContextProceed(method, args);
        }
    }

    static class ContextualTrigger extends ContextualObject implements Trigger {

        private static final Method GET_NEXT_RUNTIME_METHOD = getDeclaredMethod(Trigger.class, "getNextRunTime", LastExecution.class, Date.class);
        private static final Method SKIP_RUN_METHOD = getDeclaredMethod(Trigger.class, "skipRun", LastExecution.class, Date.class);

        ContextualTrigger(InterceptorContext interceptorContext, Trigger target) {
            super(interceptorContext, target);
        }

        @Override
        public Date getNextRunTime(LastExecution lastExecution, Date date) {
            final Object[] params = {lastExecution, date};
            try {
                return (Date) interceptorContextProceed(GET_NEXT_RUNTIME_METHOD, params);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean skipRun(LastExecution lastExecution, Date date) {
            final Object[] params = {lastExecution, date};
            try {
                return (boolean) interceptorContextProceed(SKIP_RUN_METHOD, params);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class ContextualManagedTaskListener extends ContextualObject implements ManagedTaskListener {

        private static final Method TASK_ABORTED_METHOD = getDeclaredMethod(ManagedTaskListener.class, "taskAborted", Future.class, ManagedExecutorService.class, Throwable.class);
        private static final Method TASK_DONE_METHOD = getDeclaredMethod(ManagedTaskListener.class, "taskDone", Future.class, ManagedExecutorService.class, Throwable.class);
        private static final Method TASK_STARTING_METHOD = getDeclaredMethod(ManagedTaskListener.class, "taskStarting", Future.class, ManagedExecutorService.class);
        private static final Method TASK_SUBMITTED_METHOD = getDeclaredMethod(ManagedTaskListener.class, "taskSubmitted", Future.class, ManagedExecutorService.class);

        ContextualManagedTaskListener(InterceptorContext interceptorContext, ManagedTaskListener target) {
            super(interceptorContext, target);
        }

        @Override
        public void taskAborted(Future<?> future, ManagedExecutorService managedExecutorService, Object task, Throwable throwable) {
            final Object[] params = {future, managedExecutorService, task, throwable};
            try {
                interceptorContextProceed(TASK_ABORTED_METHOD, params);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void taskDone(Future<?> future, ManagedExecutorService managedExecutorService, Object task, Throwable throwable) {
            final Object[] params = {future, managedExecutorService, task, throwable};
            try {
                interceptorContextProceed(TASK_DONE_METHOD, params);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void taskStarting(Future<?> future, ManagedExecutorService managedExecutorService, Object task) {
            final Object[] params = {future, managedExecutorService, task};
            try {
                interceptorContextProceed(TASK_STARTING_METHOD, params);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void taskSubmitted(Future<?> future, ManagedExecutorService managedExecutorService, Object task) {
            final Object[] params = {future, managedExecutorService, task};
            try {
                interceptorContextProceed(TASK_SUBMITTED_METHOD, params);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
