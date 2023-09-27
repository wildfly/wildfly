/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.interceptor;

import java.util.Map;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.jpa.container.ExtendedEntityManager;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * For SFSB life cycle management.
 * Handles the closing of XPC after last SFSB using it is destroyed.
 *
 * @author Scott Marlow
 */
public class SFSBDestroyInterceptor implements Interceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new SFSBDestroyInterceptor());

    @Override
    public Object processInvocation(InterceptorContext interceptorContext) throws Exception {
        final ComponentInstance componentInstance = interceptorContext.getPrivateData(ComponentInstance.class);
        try {
            return interceptorContext.proceed();
        } finally {
            ManagedReference entityManagerRef = (ManagedReference) componentInstance.getInstanceData(SFSBInvocationInterceptor.CONTEXT_KEY);
            if(entityManagerRef != null) {
                Map<String, ExtendedEntityManager> entityManagers = (Map<String, ExtendedEntityManager>) entityManagerRef.getInstance();
                for(Map.Entry<String, ExtendedEntityManager> entry : entityManagers.entrySet()) {
                    // close all extended persistence contexts that are referenced by the bean being destroyed
                    entry.getValue().refCountedClose();
                }
            }
        }
    }
}
