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

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.component.session.SessionBeanComponent;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.plugins.JBossSecurityContext;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * An asynchronous execution interceptor for methods returning {@link java.util.concurrent.Future}.  Because asynchronous invocations
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
                final SecurityContext securityContext = SecurityContextAssociation.getSecurityContext();
                // clone the original security context so that changes to the original security context in a separate (caller/unrelated) thread doesn't affect
                // the security context associated with the async invocation thread
                final SecurityContext clonedSecurityContext;
                if (securityContext instanceof JBossSecurityContext) {
                    clonedSecurityContext = (SecurityContext) ((JBossSecurityContext) securityContext).clone();
                } else {
                    // we can't do anything if it isn't a JBossSecurityContext so just use the original one
                    clonedSecurityContext = securityContext;
                }
                final AsyncInvocationTask task = new AsyncInvocationTask(flag) {
                    @Override
                    protected Object runInvocation() throws Exception {
                        setSecurityContextOnAssociation(clonedSecurityContext);
                        try {
                            return asyncInterceptorContext.proceed();
                        } finally {
                            clearSecurityContextOnAssociation();
                        }
                    }
                };
                asyncInterceptorContext.putPrivateData(CancellationFlag.class, flag);
                // This interceptor runs in user application's context classloader. Triggering an execute via a executor service from here can potentially lead to
                // new thread creation which will assign themselves the context classloader of the parent thread (i.e. this thread). This effectively can lead to
                // deployment's classloader leak. See https://issues.jboss.org/browse/WFLY-1375
                // To prevent this, we set the TCCL of this thread to null and then trigger the "execute" before "finally" setting the TCCL back to the original one.
                final ClassLoader oldClassLoader = WildFlySecurityManager.setCurrentContextClassLoaderPrivileged((ClassLoader) null);
                try {
                    component.getAsynchronousExecutor().execute(task);
                } finally {
                    // reset to the original TCCL
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldClassLoader);
                }
                return task;
            }
        };
    }

    private static void setSecurityContextOnAssociation(final SecurityContext sc) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            @Override
            public Void run() {
                SecurityContextAssociation.setSecurityContext(sc);
                return null;
            }
        });
    }

    private static void clearSecurityContextOnAssociation() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            @Override
            public Void run() {
                SecurityContextAssociation.clearSecurityContext();
                return null;
            }
        });
    }
}
