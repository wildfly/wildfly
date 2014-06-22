/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component.singleton;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.interceptors.AbstractEJBInterceptor;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * Responsible for associating the single component instance for a singleton bean during invocation.
 *
 * @author Jaikiran Pai
 */
public class SingletonComponentInstanceAssociationInterceptor extends AbstractEJBInterceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new SingletonComponentInstanceAssociationInterceptor());

    private SingletonComponentInstanceAssociationInterceptor() {

    }

    @Override
    public Object processInvocation(InterceptorContext interceptorContext) throws Exception {
        SingletonComponent singletonComponent = getComponent(interceptorContext, SingletonComponent.class);
        ComponentInstance singletonComponentInstance = singletonComponent.getComponentInstance();
        if (singletonComponentInstance == null) {
            throw EjbLogger.ROOT_LOGGER.componentInstanceNotAvailable(interceptorContext);
        }
        interceptorContext.putPrivateData(ComponentInstance.class, singletonComponentInstance);
        return interceptorContext.proceed();
    }
}
