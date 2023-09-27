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
 * An interceptor factory which gets an object instance from a managed resource.  A reference to the resource will be
 * attached to the given factory context key; the resource should be retained and passed to an instance of {@link
 * ManagedReferenceReleaseInterceptor} which is run during destruction.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class ComponentInstantiatorInterceptor implements Interceptor {

    private final ComponentFactory componentFactory;
    private final Object contextKey;
    private final boolean setTarget;


    /**
     * Construct a new instance.
     *
     * @param componentFactory the managed reference factory to create from
     * @param contextKey       the context key
     * @param setTarget
     */
    public ComponentInstantiatorInterceptor(final ComponentFactory componentFactory, final Object contextKey, final boolean setTarget) {
        this.setTarget = setTarget;
        if (componentFactory == null) {
            throw EeLogger.ROOT_LOGGER.nullVar("componentFactory");
        }
        if (contextKey == null) {
            throw EeLogger.ROOT_LOGGER.nullVar("contextKey");
        }
        this.componentFactory = componentFactory;
        this.contextKey = contextKey;
    }

    public Object processInvocation(final InterceptorContext context) throws Exception {
        final ComponentInstance componentInstance = context.getPrivateData(ComponentInstance.class);
        final ManagedReference existing = (ManagedReference) componentInstance.getInstanceData(contextKey);
        if (existing == null) {
            final ManagedReference reference = componentFactory.create(context);
            boolean ok = false;
            try {
                componentInstance.setInstanceData(contextKey, reference);
                if (setTarget) {
                    context.setTarget(reference.getInstance());
                }
                Object result = context.proceed();
                ok = true;
                return result;
            } finally {
                if (!ok) {
                    reference.release();
                    componentInstance.setInstanceData(contextKey, reference);
                }
            }
        } else {
            return context.proceed();
        }
    }

}
