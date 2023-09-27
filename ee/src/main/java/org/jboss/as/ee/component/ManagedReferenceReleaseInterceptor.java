/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

/**
 * An interceptor which releases a managed reference.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class ManagedReferenceReleaseInterceptor implements Interceptor {

    private final Object contextKey;

    /**
     * Construct a new instance.
     *
     * @param contextKey the context key
     */
    ManagedReferenceReleaseInterceptor(final Object contextKey) {
        if (contextKey == null) {
            throw EeLogger.ROOT_LOGGER.nullVar("contextKey");
        }
        this.contextKey = contextKey;
    }

    /**
     * {@inheritDoc}
     */
    public Object processInvocation(final InterceptorContext context) throws Exception {
        try {
            return context.proceed();
        } finally {
            final ManagedReference managedReference = (ManagedReference) context.getPrivateData(ComponentInstance.class).getInstanceData(contextKey);
            if (managedReference != null) {
                managedReference.release();
            }
        }
    }
}
