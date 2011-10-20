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

package org.jboss.as.ee.component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

/**
 * An invocation handler for a component proxy.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ProxyInvocationHandler implements InvocationHandler {

    private final Map<Method, Interceptor> interceptors;
    private final Component component;
    private final ComponentView componentView;

    /**
     * Construct a new instance.
     *
     * @param interceptors the interceptors map to use
     * @param component The component
     * @param componentView The component view
     */
    public ProxyInvocationHandler(final Map<Method, Interceptor> interceptors, Component component, ComponentView componentView) {
        this.interceptors = interceptors;
        this.component = component;
        this.componentView = componentView;
    }

    /** {@inheritDoc} */
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final Interceptor interceptor = interceptors.get(method);
        if (interceptor == null) {
            throw new NoSuchMethodError(method.toString());
        }
        final InterceptorContext context = new InterceptorContext();
        // special location for original proxy
        context.putPrivateData(Object.class, proxy);
        context.putPrivateData(Component.class, component);
        context.putPrivateData(ComponentView.class, componentView);
        context.setParameters(args);
        context.setMethod(method);
        // setup the public context data
        context.setContextData(new HashMap());
        return interceptor.processInvocation(context);
    }
}
