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

package org.jboss.as.ejb3.component.interceptors;

import java.util.concurrent.Future;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * An asynchronous execution interceptor for methods returning {@link Future}.  Because asynchronous invocations
 * necessarily run in a concurrent thread, any thread context setup interceptors should run <b>after</b> this
 * interceptor to prevent that context from becoming lost.  This interceptor should be associated with the client
 * interceptor stack.
 * <p/>
 * Cancellation notification is accomplished via the {@link CancellationFlag} private data attachment.  This interceptor
 * will create and attach a new cancellation flag, which will be set to {@code true} if the request was cancelled.
 * <p/>
 * This interceptor should only be used for local invocations.
 *
 * @author Stuart Douglas
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class AsyncFutureInterceptorFactory implements InterceptorFactory {

    public static final InterceptorFactory INSTANCE = new AsyncFutureInterceptorFactory();

    private AsyncFutureInterceptorFactory() {
    }

    @Override
    public Interceptor create(final InterceptorFactoryContext context) {

        final SessionBeanComponent component = (SessionBeanComponent) context.getContextData().get(Component.class);

        return new Interceptor() {
            @Override
            public Object processInvocation(final InterceptorContext context) throws Exception {
                final InterceptorContext asyncInterceptorContext = context.clone();
                asyncInterceptorContext.putPrivateData(InvocationType.class, InvocationType.ASYNC);
                final CancellationFlag flag = new CancellationFlag();
                final AsyncInvocationTask task = new AsyncInvocationTask( flag) {

                    @Override
                    protected Object runInvocation() throws Exception {
                        return asyncInterceptorContext.proceed();
                    }
                };
                asyncInterceptorContext.putPrivateData(CancellationFlag.class, flag);
                component.getAsynchronousExecutor().execute(task);
                return task;
            }
        };
    }

}
