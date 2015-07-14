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

package org.jboss.as.ejb3.component.interceptors;

import static org.jboss.as.ejb3.component.interceptors.StoredLogDiagnosticContext.KEY;

import java.util.Map;

import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.MDC;
import org.jboss.logging.NDC;

/**
 * An interceptor which saves the logging NDC and MDC for asynchronous invocations.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LogDiagnosticContextStorageInterceptor implements Interceptor {

    private static final LogDiagnosticContextStorageInterceptor INSTANCE = new LogDiagnosticContextStorageInterceptor();
    private static final ImmediateInterceptorFactory FACTORY = new ImmediateInterceptorFactory(INSTANCE);

    private LogDiagnosticContextStorageInterceptor() {
    }

    /**
     * Get the interceptor factory for this interceptor.
     *
     * @return the interceptor factory for this interceptor
     */
    public static ImmediateInterceptorFactory getFactory() {
        return FACTORY;
    }

    /**
     * Get this interceptor instance.
     *
     * @return this interceptor instance
     */
    public static LogDiagnosticContextStorageInterceptor getInstance() {
        return INSTANCE;
    }

    public Object processInvocation(final InterceptorContext context) throws Exception {
        final Map<String, Object> mdc = MDC.getMap();
        if(mdc != null){
            context.putPrivateData(KEY, new StoredLogDiagnosticContext(mdc, NDC.get()));
            try {
                return context.proceed();
            } finally {
                context.putPrivateData(KEY, null);
            }
        } else {
            return context.proceed();
        }
    }
}
