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

package org.jboss.as.ee.component.interceptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.Interceptors;

/**
 * @author John Bailey
 */
public class ComponentInterceptorFactories {
    private final Map<Method, List<InterceptorFactory>> classInterceptorFactories = new HashMap<Method, List<InterceptorFactory>>();
    private final Map<Method, List<InterceptorFactory>> methodInterceptorFactories = new HashMap<Method, List<InterceptorFactory>>();
    private final Map<Method, List<InterceptorFactory>> componentInterceptorFactories = new HashMap<Method, List<InterceptorFactory>>();

    public InterceptorFactory getClassLevelInterceptorFactory(final Method method) {
        final List<InterceptorFactory> interceptorFactories = classInterceptorFactories.get(method);
        if (interceptorFactories == null) {
            return Interceptors.getNullInterceptorFactory();
        }
        return Interceptors.getChainedInterceptorFactory(interceptorFactories);
    }

    public InterceptorFactory getMethodLevelInterceptorFactory(final Method method) {
        final List<InterceptorFactory> interceptorFactories = methodInterceptorFactories.get(method);
        if (interceptorFactories == null) {
            return Interceptors.getNullInterceptorFactory();
        }
        return Interceptors.getChainedInterceptorFactory(interceptorFactories);
    }

    public InterceptorFactory getComponentDefinedInterceptorFactory(final Method method) {
        final List<InterceptorFactory> interceptorFactories = componentInterceptorFactories.get(method);
        if (interceptorFactories == null) {
            return Interceptors.getNullInterceptorFactory();
        }
        return Interceptors.getChainedInterceptorFactory(interceptorFactories);
    }

    public void addClassInterceptorFactory(final Method method, final InterceptorFactory interceptorFactory) {
        final Map<Method, List<InterceptorFactory>> classInterceptorFactories = this.classInterceptorFactories;
        List<InterceptorFactory> classFactories = classInterceptorFactories.get(method);
        if (classFactories == null) {
            classFactories = new ArrayList<InterceptorFactory>();
            methodInterceptorFactories.put(method, classFactories);
        }
        classFactories.add(interceptorFactory);
    }

    public void addMethodInterceptorFactory(final Method method, final InterceptorFactory interceptorFactory) {
        final Map<Method, List<InterceptorFactory>> methodInterceptorFactories = this.methodInterceptorFactories;
        List<InterceptorFactory> methodFactories = methodInterceptorFactories.get(method);
        if (methodFactories == null) {
            methodFactories = new ArrayList<InterceptorFactory>();
            methodInterceptorFactories.put(method, methodFactories);
        }
        methodFactories.add(interceptorFactory);
    }

    public void addComponentInterceptorFactory(final Method method, final InterceptorFactory interceptorFactory) {
        final Map<Method, List<InterceptorFactory>> componentInterceptorFactories = this.componentInterceptorFactories;
        List<InterceptorFactory> componentFactories = componentInterceptorFactories.get(method);
        if (componentFactories == null) {
            componentFactories = new ArrayList<InterceptorFactory>();
            componentInterceptorFactories.put(method, componentFactories);
        }
        componentFactories.add(interceptorFactory);
    }
}
