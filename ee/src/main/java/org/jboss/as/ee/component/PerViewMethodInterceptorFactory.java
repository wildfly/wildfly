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
package org.jboss.as.ee.component;

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

import java.lang.reflect.Method;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class PerViewMethodInterceptorFactory implements InterceptorFactory {
    @Override
    public final Interceptor create(InterceptorFactoryContext context) {
        Component component = (Component) context.getContextData().get(Component.class);
        assert component != null : "component is not set in " + context;
        ComponentInstance componentInstance = (ComponentInstance) context.getContextData().get(ComponentInstance.class);
        assert componentInstance != null : "componentInstance is not set in " + context;
        Method viewMethod = (Method) context.getContextData().get(Method.class);
        assert viewMethod != null : "viewMethod is not set in " + context;
        return create(component, componentInstance, viewMethod, context);
    }

    /**
     * @param component
     * @param instance
     * @param method
     * @param context       the per-instance InterceptorFactoryContext (don't ask, look at AbstractComponent)
     * @return
     */
    protected abstract Interceptor create(Component component, ComponentInstance instance, Method method, InterceptorFactoryContext context);

    static void populate(InterceptorFactoryContext context, Component component, ComponentInstance instance, Method method) {
        assert component != null : "component is null";
        assert instance != null : "instance is null";
        assert method != null : "method is null";
        context.getContextData().put(Component.class, component);
        context.getContextData().put(ComponentInstance.class, instance);
        context.getContextData().put(Method.class, method);
    }
}
