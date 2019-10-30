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
package org.jboss.as.ejb3.component.interceptors;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * Interceptor for equals / hashCode for SLSB's and Singleton beans
 *
 * @author Stuart Douglas
 */
public class ComponentTypeIdentityInterceptorFactory implements InterceptorFactory {

    public static final ComponentTypeIdentityInterceptorFactory INSTANCE = new ComponentTypeIdentityInterceptorFactory();

    private ComponentTypeIdentityInterceptorFactory() {

    }

    @Override
    public Interceptor create(final InterceptorFactoryContext context) {
        final ComponentView componentView = (ComponentView) context.getContextData().get(ComponentView.class);
        return new ProxyTypeEqualsInterceptor(componentView);

    }

    private static class ProxyTypeEqualsInterceptor implements Interceptor {

        private final ComponentView componentView;

        public ProxyTypeEqualsInterceptor(final ComponentView componentView) {
            this.componentView = componentView;
        }

        @Override
        public Object processInvocation(final InterceptorContext context) throws Exception {
            if ((context.getMethod().getName().equals("equals"))
                    || context.getMethod().getName().equals("isIdentical")) {
                final Object other = context.getParameters()[0];
                if (other == null) {
                    return false;
                }
                final Class<?> proxyType = componentView.getProxyClass();
                return proxyType.isAssignableFrom(other.getClass());
            } else if (context.getMethod().getName().equals("hashCode")) {
                //use the identity of the component view as a hash code
                return componentView.hashCode();
            } else {
                return context.proceed();
            }

        }
    }
}
