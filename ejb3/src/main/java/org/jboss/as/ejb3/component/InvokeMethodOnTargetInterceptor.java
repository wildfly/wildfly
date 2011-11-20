/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.Interceptors;

/**
 *
 * Interceptor that directly invokes the target from the interceptor context and returns the result
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class InvokeMethodOnTargetInterceptor implements Interceptor {
    public static final Class<Object[]> PARAMETERS_KEY = Object[].class;

    private final Method method;

    InvokeMethodOnTargetInterceptor(Method method) {
        this.method = method;
    }

    public static InterceptorFactory factory(final Method method) {
        return new ImmediateInterceptorFactory(new InvokeMethodOnTargetInterceptor(method));
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        final Object instance = context.getPrivateData(ComponentInstance.class).getInstance();
        try {
            return method.invoke(instance, context.getPrivateData(PARAMETERS_KEY));
        } catch (InvocationTargetException e) {
            throw Interceptors.rethrow(e.getCause());
        }
    }
}
