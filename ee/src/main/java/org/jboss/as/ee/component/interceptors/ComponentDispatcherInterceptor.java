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

package org.jboss.as.ee.component.interceptors;

import java.lang.reflect.Method;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

/**
* @author Stuart Douglas
*/
public class ComponentDispatcherInterceptor implements Interceptor {

    private final Method componentMethod;

    public ComponentDispatcherInterceptor(final Method componentMethod) {
        this.componentMethod = componentMethod;
    }

    public Object processInvocation(final InterceptorContext context) throws Exception {
        ComponentInstance componentInstance = context.getPrivateData(ComponentInstance.class);
        if (componentInstance == null) {
            throw EeLogger.ROOT_LOGGER.noComponentInstance();
        }
        Method oldMethod = context.getMethod();
        try {
            context.setMethod(componentMethod);
            context.setTarget(componentInstance.getInstance());
            return componentInstance.getInterceptor(componentMethod).processInvocation(context);
        } finally {
            context.setMethod(oldMethod);
            context.setTarget(null);
        }
    }
}
