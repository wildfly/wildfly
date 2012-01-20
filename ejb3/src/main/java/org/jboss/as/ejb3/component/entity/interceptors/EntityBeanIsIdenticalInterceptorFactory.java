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

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * Interceptor that handles the {@link javax.ejb.EJBLocalObject#isIdentical(javax.ejb.EJBLocalObject)}
 * && {@link javax.ejb.EJBObject#isIdentical(javax.ejb.EJBObject)} methods
 *
 * @author Stuart Douglas
 */
public class EntityBeanIsIdenticalInterceptorFactory implements InterceptorFactory {

    public static final EntityBeanIsIdenticalInterceptorFactory INSTANCE = new EntityBeanIsIdenticalInterceptorFactory();

    private EntityBeanIsIdenticalInterceptorFactory() {
    }

    @Override
    public Interceptor create(final InterceptorFactoryContext context) {
        final ComponentView componentView = (ComponentView) context.getContextData().get(ComponentView.class);
        return new EntityIsIdenticalInterceptor(componentView);

    }

    private class EntityIsIdenticalInterceptor implements Interceptor {

        private final ComponentView componentView;

        public EntityIsIdenticalInterceptor(final ComponentView componentView) {
            this.componentView = componentView;
        }

        @Override
        public Object processInvocation(final InterceptorContext context) throws Exception {
            final EntityBeanComponentInstance instance = (EntityBeanComponentInstance) context.getPrivateData(ComponentInstance.class);
            try {
                final Object primaryKey = context.getPrivateData(EntityBeanComponent.PRIMARY_KEY_CONTEXT_KEY);
                final Object other = context.getParameters()[0];
                if (!componentView.getViewClass().isAssignableFrom(other.getClass())) {
                    return false;
                }
                if (context.getMethod().getParameterTypes()[0].equals(EJBLocalObject.class)) {
                    return ((EJBLocalObject) other).getPrimaryKey().equals(primaryKey);
                } else {
                    return ((EJBObject) other).getPrimaryKey().equals(primaryKey);
                }
            } finally {
                instance.getComponent().getCache().release(instance, true);
            }
        }
    }
}
