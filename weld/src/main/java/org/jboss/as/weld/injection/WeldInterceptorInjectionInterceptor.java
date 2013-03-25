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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * Class that performs CDI injection and calls initializer methods on interceptor classes after resource injection
 * has been run
 *
 * @author Stuart Douglas
 */
public class WeldInterceptorInjectionInterceptor implements Interceptor {

    final Map<Class<?>, AtomicReference<ManagedReference>> interceptors;
    final WeldManagedReferenceFactory managedReferenceFactory;

    public WeldInterceptorInjectionInterceptor(final Map<Class<?>, AtomicReference<ManagedReference>> interceptors, final WeldManagedReferenceFactory managedReferenceFactory) {
        this.interceptors = interceptors;
        this.managedReferenceFactory = managedReferenceFactory;
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        WeldInjectionContext injectionContext = context.getPrivateData(WeldInjectionContext.class);
        //now inject the interceptors
        for (final Map.Entry<Class<?>, AtomicReference<ManagedReference>> entry : interceptors.entrySet()) {
            final ManagedReference instance = entry.getValue().get();
            if (instance != null) {
                injectionContext.injectInterceptor(instance.getInstance());
            }
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
            final Map<Class<?>, AtomicReference<ManagedReference>> interceptors = new HashMap<Class<?>, AtomicReference<ManagedReference>>();
            for (Class<?> clazz : interceptorClasses) {
                final AtomicReference<ManagedReference> interceptor = (AtomicReference<ManagedReference>) context.getContextData().get(clazz);
                if (interceptor != null) {
                    interceptors.put(clazz, interceptor);
                }

            }
            WeldManagedReferenceFactory managedReferenceFactory = null;
            if (configuration.getInstanceFactory() instanceof WeldManagedReferenceFactory) {
                managedReferenceFactory = (WeldManagedReferenceFactory) configuration.getInstanceFactory();
            }
            return new WeldInterceptorInjectionInterceptor(interceptors, managedReferenceFactory);
        }
    }
}
