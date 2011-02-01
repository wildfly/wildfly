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
import java.util.HashMap;
import java.util.Map;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * @author John Bailey
 */
public class ComponentInterceptorFactories {
    private final Map<Method, InterceptorFactory> methodInterceptorFactories;

    public ComponentInterceptorFactories(Map<Method, InterceptorFactory> methodInterceptorFactories) {
        this.methodInterceptorFactories = methodInterceptorFactories;
    }

    public Map<Method, Interceptor> createInstance(final InterceptorFactoryContext interceptorFactoryContext) {
        final Map<Method, Interceptor> methodInterceptors = new HashMap<Method, Interceptor>();
        for (Map.Entry<Method, InterceptorFactory> entry : methodInterceptorFactories.entrySet()) {
            methodInterceptors.put(entry.getKey(), entry.getValue().create(interceptorFactoryContext));
        }
        return methodInterceptors;
    }
}
