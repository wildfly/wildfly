/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
