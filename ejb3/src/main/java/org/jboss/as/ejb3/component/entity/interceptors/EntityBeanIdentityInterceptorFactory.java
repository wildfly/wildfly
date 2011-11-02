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
package org.jboss.as.ejb3.component.entity.interceptors;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * Interceptor for equals / hashCode for Entity beans.
 *
 * @author Stuart Douglas
 * @author John Bailey
 */
public class EntityBeanIdentityInterceptorFactory implements InterceptorFactory {
    public static final EntityBeanIdentityInterceptorFactory INSTANCE = new EntityBeanIdentityInterceptorFactory();

    public Interceptor create(final InterceptorFactoryContext context) {
        final ComponentView componentView = (ComponentView) context.getContextData().get(ComponentView.class);
        return new EntityIdentityInterceptor(componentView);
    }

    private class EntityIdentityInterceptor implements Interceptor {

        private final ComponentView componentView;

        public EntityIdentityInterceptor(final ComponentView componentView) {
            this.componentView = componentView;
        }

        public Object processInvocation(final InterceptorContext context) throws Exception {
            final Object primaryKey = context.getPrivateData(EntityBeanComponent.PRIMARY_KEY_CONTEXT_KEY);

            if (context.getMethod().getName().equals("equals") && context.getParameters().length == 1 && context.getMethod().getParameterTypes()[0] == Object.class) {
                final Object other = context.getParameters()[0];
                final Class<?> proxyType = componentView.getProxyClass();
                if( proxyType.isAssignableFrom(other.getClass())) {
                    //now we know that this is an ejb for the correct component view
                    //as digging out the session id from the proxy object is not really
                    //a viable option, we invoke equals() for the other instance with a
                    //PrimaryKeyHolder as the other side
                    return other.equals(new PrimaryKeyHolder(primaryKey));
                } else if(other instanceof PrimaryKeyHolder) {
                    return primaryKey.equals(((PrimaryKeyHolder) other).primaryKey);
                } else {
                    return false;
                }
            } else if (context.getMethod().getName().equals("hashCode")) {
                //use the identity of the component view as a hash code
                return primaryKey.hashCode();
            } else {
                return context.proceed();
            }
        }
    }

    private static class PrimaryKeyHolder {
        private final Object primaryKey;

        public PrimaryKeyHolder(final Object primaryKey) {
            this.primaryKey = primaryKey;
        }
    }
}
