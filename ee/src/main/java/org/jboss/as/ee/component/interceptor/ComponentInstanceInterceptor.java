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

package org.jboss.as.ee.component.interceptor;

import java.lang.reflect.Method;
import java.util.Map;
import javax.interceptor.InvocationContext;
import org.jboss.as.ee.component.Component;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.SimpleInterceptorFactoryContext;
import org.jboss.invocation.SimpleInvocationContext;

/**
 * @author John Bailey
 */
public class ComponentInstanceInterceptor<T> implements Interceptor {
    private final Component<T> component;
    private final ComponentInterceptorFactories methodInterceptorFactories;
    private T instance;
    private Map<Method, Interceptor> methodInterceptors;

    public ComponentInstanceInterceptor(final Component<T> component, final ComponentInterceptorFactories methodInterceptorFactories) {
        this.component = component;
        this.methodInterceptorFactories = methodInterceptorFactories;
    }

    @SuppressWarnings("unchecked")
    public Object processInvocation(final InvocationContext context) throws Exception {
        final Method method = context.getMethod();
        final Object[] params = context.getParameters();

        final T instance = getInstance();
        final Map<Method, Interceptor> methodInterceptors = getMethodInterceptor(instance);

        final Interceptor methodInterceptor = methodInterceptors.get(method);
        if (methodInterceptor == null) {
            return context.proceed();
        }
        return methodInterceptor.processInvocation(new SimpleInvocationContext(instance, method, params, context.getContextData(), null));
    }

    private synchronized Map<Method, Interceptor> getMethodInterceptor(final T componentInstance) {
        if (methodInterceptors == null) {
            final ComponentInterceptorFactories methodInterceptorFactories = this.methodInterceptorFactories;
            final InterceptorFactoryContext interceptorFactoryContext = new SimpleInterceptorFactoryContext();
            interceptorFactoryContext.getContextData().put(componentInstance.getClass(), componentInstance);
            methodInterceptors = methodInterceptorFactories.createInstance(interceptorFactoryContext);
        }
        return methodInterceptors;
    }

    public synchronized T getInstance() {
        if (instance == null) {
            instance = component.getInstance();
        }
        return instance;
    }
}
