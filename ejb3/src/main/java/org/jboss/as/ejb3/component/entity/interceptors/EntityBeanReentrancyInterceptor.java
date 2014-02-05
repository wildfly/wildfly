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
import org.jboss.as.ee.component.ComponentInstanceInterceptorFactory;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 *
 *
 * @author Stuart Douglas
 */
public class EntityBeanReentrancyInterceptor implements Interceptor{


    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        EntityBeanComponentInstance instance = (EntityBeanComponentInstance) context.getPrivateData(ComponentInstance.class);
        if(instance.isInvocationInProgress()) {
            throw EjbLogger.ROOT_LOGGER.failToReacquireLockForNonReentrant(context.getPrivateData(ComponentInstance.class));
        }
        instance.setInvocationInProgress(true);
        try {
            return context.proceed();
        } finally {
            instance.setInvocationInProgress(false);
        }
    }


    public static final InterceptorFactory FACTORY = new ComponentInstanceInterceptorFactory() {

        @Override
        protected org.jboss.invocation.Interceptor create(final Component component, final InterceptorFactoryContext context) {
            return new EntityBeanReentrancyInterceptor();
        }
    };


}
