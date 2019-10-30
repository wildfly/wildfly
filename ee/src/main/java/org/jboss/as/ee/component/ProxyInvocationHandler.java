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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * An invocation handler for a component proxy.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ProxyInvocationHandler implements InvocationHandler {

    private final Map<Method, Interceptor> interceptors;
    private final ComponentView componentView;
    private final ComponentClientInstance instance;

    /**
     * Construct a new instance.
     *
     * @param interceptors the interceptors map to use
     * @param instance The view instane data
     * @param componentView The component view
     */
    public ProxyInvocationHandler(final Map<Method, Interceptor> interceptors, ComponentClientInstance instance, ComponentView componentView) {
        this.interceptors = interceptors;
        this.instance = instance;
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
        context.putPrivateData(Component.class, componentView.getComponent());
        context.putPrivateData(ComponentView.class, componentView);
        context.putPrivateData(SecurityDomain.class, WildFlySecurityManager.isChecking() ?
                AccessController.doPrivileged((PrivilegedAction<SecurityDomain>) SecurityDomain::getCurrent) :
                SecurityDomain.getCurrent());
        instance.prepareInterceptorContext(context);
        context.setParameters(args);
        context.setMethod(method);
        // setup the public context data
        context.setContextData(new HashMap<String, Object>());
        context.setBlockingCaller(true);
        return interceptor.processInvocation(context);
    }
}
