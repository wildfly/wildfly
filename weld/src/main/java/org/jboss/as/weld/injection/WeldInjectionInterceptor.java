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
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

import java.util.Set;

/**
 * Class that performs CDI injection and calls initializer methods after resource injection
 * has been run
 *
 * @author Stuart Douglas
 */
public class WeldInjectionInterceptor implements Interceptor {

    private final WeldManagedReferenceFactory weldManagedReferenceFactory;
    private final Set<Class<?>> interceptorClasses;

    public WeldInjectionInterceptor(WeldManagedReferenceFactory weldManagedReferenceFactory, Set<Class<?>> interceptorClasses) {
        this.weldManagedReferenceFactory = weldManagedReferenceFactory;
        this.interceptorClasses = interceptorClasses;
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        ComponentInstance componentInstance = context.getPrivateData(ComponentInstance.class);
        final ManagedReference managedReference = (ManagedReference) componentInstance.getInstanceData(BasicComponentInstance.INSTANCE_KEY);
        if (managedReference instanceof WeldManagedReference) {
            final WeldManagedReference reference = (WeldManagedReference) managedReference;
            reference.getInjectionTarget().inject(managedReference.getInstance(), reference.getContext());
            //now inject the interceptors
            for (final Class<?> interceptorClass : interceptorClasses) {
                final ManagedReference instance = (ManagedReference) componentInstance.getInstanceData(interceptorClass);
                if (instance != null) {
                    reference.injectInterceptor(interceptorClass, instance.getInstance());
                }
            }
        } else if(weldManagedReferenceFactory != null){
            //this component was not created by the managed reference factory, this can happen in the case of JSF managed beans
            final ManagedReference newReference = weldManagedReferenceFactory.injectExistingReference(managedReference);
            componentInstance.setInstanceData(BasicComponentInstance.INSTANCE_KEY, newReference);
        }
        return context.proceed();
    }
}
