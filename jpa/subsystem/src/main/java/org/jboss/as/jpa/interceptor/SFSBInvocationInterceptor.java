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
import org.jboss.as.jpa.container.SFSBCallStack;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * Stateful session bean Invocation interceptor that is responsible for the SFSBCallStack being set for each
 * SFSB invocation that JPA is interested in.
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
