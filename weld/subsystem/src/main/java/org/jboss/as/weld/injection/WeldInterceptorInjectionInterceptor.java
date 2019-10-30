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

import java.util.Set;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

/**
 * Class that performs CDI injection and calls initializer methods on interceptor classes after resource injection
 * has been run
 *
 * @author Stuart Douglas
 */
public class WeldInterceptorInjectionInterceptor implements Interceptor {

    private final Set<Class<?>> interceptorClasses;

    public WeldInterceptorInjectionInterceptor(final Set<Class<?>> interceptorClasses) {
        this.interceptorClasses = interceptorClasses;
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        WeldInjectionContext injectionContext = context.getPrivateData(WeldInjectionContext.class);
        final ComponentInstance componentInstance = context.getPrivateData(ComponentInstance.class);
        //now inject the interceptors
        for (final Class<?> interceptorClass : interceptorClasses) {
            final ManagedReference instance = (ManagedReference) componentInstance.getInstanceData(interceptorClass);
            if (instance != null) {
                injectionContext.injectInterceptor(instance.getInstance());
            }
        }
        return context.proceed();
    }
}
