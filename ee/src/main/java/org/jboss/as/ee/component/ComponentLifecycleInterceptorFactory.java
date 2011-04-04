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

import java.util.List;

/**
 * Interceptor factory that forms the end point of the component lifecycle
 * chain.
 *
 * @author Stuart Douglas
 */
public class ComponentLifecycleInterceptorFactory implements InterceptorFactory {

    private final List<ComponentLifecycle> lifecycleMethods;

    public ComponentLifecycleInterceptorFactory(List<ComponentLifecycle> lifecycleMethods) {
        this.lifecycleMethods = lifecycleMethods;
    }

    @Override
    public Interceptor create(InterceptorFactoryContext context) {
        return new ComponentLifecycleInterceptor((ComponentInstance)context.getContextData().get(AbstractComponent.COMPONENT_INSTANCE_KEY));
    }

    private class ComponentLifecycleInterceptor implements Interceptor {

        private final ComponentInstance instance;

        public ComponentLifecycleInterceptor(ComponentInstance instance) {
            this.instance = instance;
        }

        @Override
        public Object processInvocation(InterceptorContext context) throws Exception {
            for(ComponentLifecycle method : lifecycleMethods) {
                method.invoke(instance);
            }
            return null;
        }
    }
}
