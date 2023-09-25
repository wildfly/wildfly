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
 * An interceptor which restores the saved logging NDC and MDC for asynchronous invocations.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LogDiagnosticContextRecoveryInterceptor implements Interceptor {

    private static final LogDiagnosticContextRecoveryInterceptor INSTANCE = new LogDiagnosticContextRecoveryInterceptor();
    private static final ImmediateInterceptorFactory FACTORY = new ImmediateInterceptorFactory(INSTANCE);

    private LogDiagnosticContextRecoveryInterceptor() {
    }

    /**
     * Get the interceptor factory.
     *
     * @return the interceptor factory
     */
    public static ImmediateInterceptorFactory getFactory() {
        return FACTORY;
    }

    /**
     * Get the interceptor instance.
     *
     * @return the interceptor instance
     */
    public static LogDiagnosticContextRecoveryInterceptor getInstance() {
        return INSTANCE;
    }

    public Object processInvocation(final InterceptorContext context) throws Exception {
        final Map<String, Object> mdc = MDC.getMap();
        if (mdc != null) {
            for (String str : mdc.keySet()) {
                MDC.remove(str);
            }
        }
        final StoredLogDiagnosticContext data = (StoredLogDiagnosticContext) context.getPrivateData(KEY);
        context.putPrivateData(KEY, null);
        if (data != null && data.getMdc() != null) {
            for (Map.Entry<String, Object> entry : data.getMdc().entrySet()) {
                MDC.put(entry.getKey(), entry.getValue());
            }
            final int depth = NDC.getDepth();
            NDC.push(data.getNdc());
            try {
                return context.proceed();
            } finally {
                NDC.setMaxDepth(depth);
                for (String str : MDC.getMap().keySet()) {
                    MDC.remove(str);
                }
            }
        }
        return context.proceed();
    }
}
