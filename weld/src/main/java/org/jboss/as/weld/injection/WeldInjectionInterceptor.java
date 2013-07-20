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

import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * Class that performs CDI injection and calls initializer methods after resource injection
 * has been run
 *
 * @author Stuart Douglas
 */
public class WeldInjectionInterceptor implements Interceptor {

    final AtomicReference<ManagedReference> targetReference;

    public WeldInjectionInterceptor(final AtomicReference<ManagedReference> targetReference) {
        this.targetReference = targetReference;
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        WeldInjectionContext injectionContext = context.getPrivateData(WeldInjectionContext.class);
        ManagedReference reference = targetReference.get();
        if (reference == null) {
            return null;
        }
        injectionContext.inject(reference.getInstance());
        return context.proceed();
    }

    public static class Factory implements InterceptorFactory {

        final ComponentConfiguration configuration;

        public Factory(final ComponentConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        public Interceptor create(final InterceptorFactoryContext context) {
            final AtomicReference<ManagedReference> targetReference = (AtomicReference<ManagedReference>) context.getContextData().get(BasicComponentInstance.INSTANCE_KEY);
            return new WeldInjectionInterceptor(targetReference);
        }
    }
}
