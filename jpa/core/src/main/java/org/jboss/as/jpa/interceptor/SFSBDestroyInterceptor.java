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
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.jpa.container.ReferenceCountedEntityManager;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * For SFSB life cycle management.
 * Handles the closing of XPC after last SFSB using it is destroyed.
 *
 * @author Scott Marlow
 */
public class SFSBDestroyInterceptor implements Interceptor {

    private final Map<String, ReferenceCountedEntityManager> entityManagers;

    private SFSBDestroyInterceptor(final Map<String, ReferenceCountedEntityManager> entityManagers) {
        this.entityManagers = entityManagers;
    }

    @Override
    public Object processInvocation(InterceptorContext interceptorContext) throws Exception {
        try {
            return interceptorContext.proceed();
        } finally {
            for(Map.Entry<String, ReferenceCountedEntityManager> entry : entityManagers.entrySet()) {
                entry.getValue().close();
            }
        }
    }

    public static class Factory implements InterceptorFactory {

        @Override
        public Interceptor create(final InterceptorFactoryContext context) {
            return new SFSBDestroyInterceptor((Map<String, ReferenceCountedEntityManager>) ((AtomicReference<ManagedReference>) context.getContextData().get(SFSBInvocationInterceptor.CONTEXT_KEY)).get().getInstance());
        }
    }
}
