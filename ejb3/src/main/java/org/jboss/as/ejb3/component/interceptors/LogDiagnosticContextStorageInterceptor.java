/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
