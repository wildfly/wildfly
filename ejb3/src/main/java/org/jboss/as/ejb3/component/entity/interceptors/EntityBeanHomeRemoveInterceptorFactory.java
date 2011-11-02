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

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import javax.ejb.RemoveException;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.interceptors.AbstractEJBInterceptor;
import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * @author John Bailey
 */
public class EntityBeanHomeRemoveInterceptorFactory implements InterceptorFactory {
    private static final Object[] EMPTY = {};
    private final Method ejbRemove;

    public EntityBeanHomeRemoveInterceptorFactory(final Method ejbRemove) {
        this.ejbRemove = ejbRemove;
    }

    public Interceptor create(final InterceptorFactoryContext context) {
        return new AbstractEJBInterceptor() {
            public Object processInvocation(final InterceptorContext context) throws Exception {
                final EntityBeanComponent component = getComponent(context, EntityBeanComponent.class);
                final EntityBeanComponentInstance instance = component.getCache().get(context.getParameters()[0]);
                final Method oldMethod = context.getMethod();
                final Object[] oldParams = context.getParameters();
                try {
                    context.putPrivateData(ComponentInstance.class, instance);
                    context.setMethod(ejbRemove);
                    context.setParameters(EMPTY);
                    context.setTarget(instance.getInstance());
                    instance.getInterceptor(ejbRemove).processInvocation(context);
                } finally {
                    context.setMethod(oldMethod);
                    context.setParameters(oldParams);
                    context.setTarget(null);
                    context.putPrivateData(ComponentInstance.class, null);
                }
                afterRemove(instance);
                return null;
            }
        };
    }

    protected void afterRemove(final EntityBeanComponentInstance instance) throws RemoveException, RemoteException {
        instance.setRemoved(true);
    }
}
