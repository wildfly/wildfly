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
