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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.jpa.container.ReferenceCountedEntityManager;
import org.jboss.as.jpa.container.CreatedEntityManagers;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.msc.value.ImmediateValue;

/**
 * For SFSB life cycle management.
 * Handles the (@PostConstruct time) creation of the extended persistence context (XPC).
 *
 * @author Scott Marlow
 */
public class SFSBCreateInterceptor implements Interceptor {

    private final Map<String, ReferenceCountedEntityManager> entityManagers;

    private SFSBCreateInterceptor(final Map<String, ReferenceCountedEntityManager> entityManagers) {
        this.entityManagers = entityManagers;
    }

    @Override
    public Object processInvocation(InterceptorContext interceptorContext) throws Exception {
        final List<ReferenceCountedEntityManager> ems = CreatedEntityManagers.getDeferredEntityManagers();
        for (ReferenceCountedEntityManager e : ems) {
            entityManagers.put(e.getEntityManager().getScopedPuName(), e);
        }
        return interceptorContext.proceed();
    }


    public static class Factory implements InterceptorFactory {


        @Override
        public Interceptor create(final InterceptorFactoryContext context) {
            HashMap<String, ReferenceCountedEntityManager> entityManagers;
            if (context.getContextData().containsKey(SFSBInvocationInterceptor.CONTEXT_KEY)) {
                entityManagers = (HashMap<String, ReferenceCountedEntityManager>) ((AtomicReference<ManagedReference>) context.getContextData().get(SFSBInvocationInterceptor.CONTEXT_KEY)).get().getInstance();
            } else {
                entityManagers = new HashMap<String, ReferenceCountedEntityManager>();
                context.getContextData().put(SFSBInvocationInterceptor.CONTEXT_KEY, new AtomicReference<ManagedReference>(new ValueManagedReference(new ImmediateValue<Object>(entityManagers))));
            }
            return new SFSBCreateInterceptor(entityManagers);
        }
    }
}
