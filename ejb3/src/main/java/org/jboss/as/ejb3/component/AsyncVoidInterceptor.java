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

import java.util.concurrent.Executor;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.Logger;

/**
 * An asynchronous execution interceptor for methods returning {@code void}.  Because asynchronous invocations
 * necessarily run in a concurrent thread, any thread context setup interceptors should run <b>after</b> this
 * interceptor to prevent that context from becoming lost.  This interceptor should be associated with the client
 * interceptor stack.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class AsyncVoidInterceptor implements Interceptor {

    private static final Logger log = Logger.getLogger("org.jboss.as.ejb3.component.async");

    private final Executor executor;

    public AsyncVoidInterceptor(final Executor executor) {
        this.executor = executor;
    }

    /**
     * {@inheritDoc}
     */
    public Object processInvocation(final InterceptorContext context) throws Exception {
        executor.execute(new Task(context.clone()));
        return null;
    }

    private static class Task implements Runnable {
        private final InterceptorContext context;

        private Task(final InterceptorContext context) {
            this.context = context;
        }

        public void run() {
            try {
                context.proceed();
            } catch (Exception e) {
                log.error("Asynchronous invocation failed", e);
            }
            return;
        }
    }
}
