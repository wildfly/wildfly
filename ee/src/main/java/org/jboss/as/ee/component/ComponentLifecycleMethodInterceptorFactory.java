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
package org.jboss.as.ee.component;

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Interceptor factory that forms the end point of the component lifecycle
 * chain.
 * <p/>
 * It invokes lifecycle methods directly on the component instance
 *
 * @author Stuart Douglas
 */
public class ComponentLifecycleMethodInterceptorFactory implements InterceptorFactory {

    private final List<Method> lifecycleMethods;

    public ComponentLifecycleMethodInterceptorFactory(List<Method> lifecycleMethods) {
        this.lifecycleMethods = lifecycleMethods;
    }

    @Override
    public Interceptor create(InterceptorFactoryContext context) {
        return new ComponentLifecycleInterceptor((Object)context.getContextData().get(AbstractComponent.INSTANCE_KEY));
    }

    private class ComponentLifecycleInterceptor implements Interceptor {

        private final Object instance;

        public ComponentLifecycleInterceptor(Object instance) {
            this.instance = instance;
        }

        @Override
        public Object processInvocation(InterceptorContext context) throws Exception {
            for(Method method : lifecycleMethods) {
                method.invoke(instance);
            }
            return null;
        }
    }
}
