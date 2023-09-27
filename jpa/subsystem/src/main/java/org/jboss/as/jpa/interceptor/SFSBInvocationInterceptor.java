/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.interceptor;

import java.util.Map;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.jpa.container.ExtendedEntityManager;
import org.jboss.as.jpa.container.SFSBCallStack;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * Stateful session bean Invocation interceptor that is responsible for the SFSBCallStack being set for each
 * SFSB invocation that Jakarta Persistence is interested in.
 *
 * @author Scott Marlow
 */
public class SFSBInvocationInterceptor implements Interceptor {

    public static final String CONTEXT_KEY = "org.jboss.as.jpa.InterceptorContextKey";

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new SFSBInvocationInterceptor());

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final ComponentInstance componentInstance = context.getPrivateData(ComponentInstance.class);
        ManagedReference entityManagerRef = (ManagedReference) componentInstance.getInstanceData(SFSBInvocationInterceptor.CONTEXT_KEY);
        if(entityManagerRef != null) {
            Map<String, ExtendedEntityManager> entityManagers = (Map<String, ExtendedEntityManager>) entityManagerRef.getInstance();
            SFSBCallStack.pushCall(entityManagers);
        }
        try {
            return context.proceed();   // call the next interceptor or target
        } finally {
            if(entityManagerRef != null) {
                SFSBCallStack.popCall();
            }
        }
    }
}
