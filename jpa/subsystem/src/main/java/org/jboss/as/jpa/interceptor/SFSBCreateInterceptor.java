/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.interceptor;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.jpa.container.ExtendedEntityManager;
import org.jboss.as.jpa.container.CreatedEntityManagers;
import org.jboss.as.naming.ImmediateManagedReference;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * For SFSB life cycle management.
 * Handles the (@PostConstruct time) creation of the extended persistence context (XPC).
 *
 * @author Scott Marlow
 */
public class SFSBCreateInterceptor implements Interceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new SFSBCreateInterceptor());

    @Override
    public Object processInvocation(InterceptorContext interceptorContext) throws Exception {
        ComponentInstance componentInstance = interceptorContext.getPrivateData(ComponentInstance.class);
        Map<String, ExtendedEntityManager> entityManagers = null;
        if(componentInstance.getInstanceData(SFSBInvocationInterceptor.CONTEXT_KEY) == null) {
            // Get all of the extended persistence contexts in use by the bean (some of which may of been inherited from
            // other beans).
            entityManagers = new HashMap<String, ExtendedEntityManager>();
            componentInstance.setInstanceData(SFSBInvocationInterceptor.CONTEXT_KEY, new ImmediateManagedReference(entityManagers));
        } else {
            ManagedReference entityManagerRef = (ManagedReference) componentInstance.getInstanceData(SFSBInvocationInterceptor.CONTEXT_KEY);
            entityManagers = (Map<String, ExtendedEntityManager>)entityManagerRef.getInstance();
        }
        final ExtendedEntityManager[] ems = CreatedEntityManagers.getDeferredEntityManagers();
        for (ExtendedEntityManager e : ems) {
            entityManagers.put(e.getScopedPuName(), e);
        }
        return interceptorContext.proceed();
    }
}
