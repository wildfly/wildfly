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
package org.jboss.as.ejb3.component.entity.interceptors;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

import java.lang.reflect.Method;

/**
 * Interceptor that hooks up home business methods for entity beans
 * <p/>
 * This is a view level interceptor that should be attached to business methods on the home interface.
 *
 * @author Stuart Douglas
 */
public class EntityBeanHomeMethodInterceptorFactory implements InterceptorFactory {

    private final Method businessMethod;

    public EntityBeanHomeMethodInterceptorFactory(final Method businessMethod) {
        this.businessMethod = businessMethod;
    }

    @Override
    public Interceptor create(final InterceptorFactoryContext context) {

        final EntityBeanComponent component = (EntityBeanComponent) context.getContextData().get(Component.class);

        return new Interceptor() {
            @Override
            public Object processInvocation(final InterceptorContext context) throws Exception {

                //grab a bean from the pool to invoke the business method on
                final EntityBeanComponentInstance instance = component.getPool().get();
                final Object result;
                final InvocationType invocationType = context.getPrivateData(InvocationType.class);
                try {
                    context.putPrivateData(InvocationType.class, InvocationType.HOME_METHOD);
                    //forward the invocation to the component interceptor chain
                    Method oldMethod = context.getMethod();
                    try {
                        context.putPrivateData(ComponentInstance.class, instance);
                        context.setMethod(businessMethod);
                        context.setTarget(instance.getInstance());
                        return  instance.getInterceptor(businessMethod).processInvocation(context);
                    } finally {
                        context.setMethod(oldMethod);
                        context.setTarget(null);
                        context.putPrivateData(ComponentInstance.class, null);
                    }
                } finally {
                    context.putPrivateData(InvocationType.class, invocationType);
                    component.getPool().release(instance);
                }
            }

        };
    }
}
