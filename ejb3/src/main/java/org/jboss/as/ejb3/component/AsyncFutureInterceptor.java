/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.threads.AsyncFutureTask;

/**
 * An asynchronous execution interceptor for methods returning {@link Future}.  Because asynchronous invocations
 * necessarily run in a concurrent thread, any thread context setup interceptors should run <b>after</b> this
 * interceptor to prevent that context from becoming lost.  This interceptor should be associated with the client
 * interceptor stack.
 * <p>
 * Cancellation notification is accomplished via the {@link CancellationFlag} private data attachment.  This interceptor
 * will create and attach a new cancellation flag, which will be set to {@code true} if the request was cancelled.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class AsyncFutureInterceptor implements Interceptor {

    private final Executor executor;

    public AsyncFutureInterceptor(final Executor executor) {
        this.executor = executor;
    }

    /** {@inheritDoc} */
    public Object processInvocation(final InterceptorContext context) throws Exception {
        final InterceptorContext asyncInterceptorContext = context.clone();
        final CancellationFlag flag = new CancellationFlag();
        final Task task = new Task(executor, asyncInterceptorContext, flag);
        asyncInterceptorContext.putPrivateData(CancellationFlag.class, flag);
        executor.execute(task);
        return task;
    }

    private static class Task extends AsyncFutureTask<Object> implements Runnable {
        private final InterceptorContext context;
        private final CancellationFlag flag;

        private Task(final Executor executor, final InterceptorContext context, final CancellationFlag flag) {
            super(executor);
            this.context = context;
            this.flag = flag;
        }

        public void asyncCancel(final boolean interruptionDesired) {
            if (interruptionDesired) {
                flag.set(true);
            }
        }

        public void run() {
            Object result;
            try {
                result = context.proceed();
            } catch (Exception e) {
                setFailed(e);
                return;
            }
            Future<?> asyncResult = (Future<?>) result;
            try {
                result = asyncResult.get();
            } catch (InterruptedException e) {
                setFailed(new IllegalStateException(e));
                return;
            } catch (ExecutionException e) {
                try {
                    throw e.getCause();
                } catch (Exception ex) {
                    setFailed(ex);
                    return;
                } catch (Throwable throwable) {
                    setFailed(new UndeclaredThrowableException(throwable));
                    return;
                }
            }
            setResult(result);
            return;
        }
    }
}
