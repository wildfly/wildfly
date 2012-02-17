/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld.injection;

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class that performs CDI injection and calls initializer methods after resource injection
 * has been run
 *
 * @author Stuart Douglas
 */
public class WeldInjectionInterceptor implements Interceptor {

    final AtomicReference<ManagedReference> targetReference;
    final Map<Class<?>, AtomicReference<ManagedReference>> interceptors;
    final WeldManagedReferenceFactory managedReferenceFactory;

    public WeldInjectionInterceptor(final AtomicReference<ManagedReference> targetReference, final Map<Class<?>, AtomicReference<ManagedReference>> interceptors, final WeldManagedReferenceFactory managedReferenceFactory) {
        this.targetReference = targetReference;
        this.interceptors = interceptors;
        this.managedReferenceFactory = managedReferenceFactory;
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        final ManagedReference managedReference = targetReference.get();
        if (managedReference instanceof WeldManagedReference) {
            final WeldManagedReference reference = (WeldManagedReference) managedReference;
            reference.getInjectionTarget().inject(targetReference.get().getInstance(), reference.getContext());
            //now inject the interceptors
            for (final Map.Entry<Class<?>, AtomicReference<ManagedReference>> entry : interceptors.entrySet()) {
                final ManagedReference instance = entry.getValue().get();
                if (instance != null) {
                    reference.injectInterceptor(entry.getKey(), instance.getInstance());
                }
            }
        } else if(managedReferenceFactory != null){
            //this component was not created by the managed reference factory, this can happen in the case of JSF managed beans
            final ManagedReference newReference = managedReferenceFactory.injectExistingReference(managedReference);
            targetReference.set(newReference);
        }
        return context.proceed();
    }

    public static class Factory implements InterceptorFactory {

        final ComponentConfiguration configuration;
        final Set<Class<?>> interceptorClasses;

        public Factory(final ComponentConfiguration configuration, final Set<Class<?>> interceptorClasses) {
            this.configuration = configuration;
            this.interceptorClasses = interceptorClasses;
        }

        @Override
        public Interceptor create(final InterceptorFactoryContext context) {
            final AtomicReference<ManagedReference> targetReference = (AtomicReference<ManagedReference>) context.getContextData().get(BasicComponentInstance.INSTANCE_KEY);
            final Map<Class<?>, AtomicReference<ManagedReference>> interceptors = new HashMap<Class<?>, AtomicReference<ManagedReference>>();
            for (Class<?> clazz : interceptorClasses) {
                final AtomicReference<ManagedReference> interceptor = (AtomicReference<ManagedReference>) context.getContextData().get(clazz);
                if(interceptor != null) {
                    interceptors.put(clazz, interceptor);
                }

            }
            WeldManagedReferenceFactory managedReferenceFactory = null;
            if(configuration.getInstanceFactory() instanceof WeldManagedReferenceFactory) {
                managedReferenceFactory = (WeldManagedReferenceFactory) configuration.getInstanceFactory();
            }
            return new WeldInjectionInterceptor(targetReference, interceptors, managedReferenceFactory);
        }
    }
}
