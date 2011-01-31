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

import org.jboss.as.ee.component.injection.ResourceInjectableConfiguration;

/**
 * Configuration for an interceptor bound to a managed bean class.
 *
 * @author John E. Bailey
 */
public class MethodInterceptorConfiguration extends ResourceInjectableConfiguration {
    private final String interceptorClassName;
    private final String methodName;
    private final MethodInterceptorFilter methodFilter;

    /**
     * Create an instance with the interceptor class and the resource configurations.
     *
     * @param interceptorClass         The interceptor class name
     * @param methodName               The interceptor method name
     * @param methodFilter             The method filter
     */
    public MethodInterceptorConfiguration(final String interceptorClass, final String methodName, final MethodInterceptorFilter methodFilter) {
        this.interceptorClassName = interceptorClass;
        this.methodName = methodName;
        this.methodFilter = methodFilter;
    }

    /**
     * Get the interceptor class.
     *
     * @return The interceptor class
     */
    public String getInterceptorClassName() {
        return interceptorClassName;
    }

    /**
     * Get the interceptor method name.
     *
     * @return The interceptor method name
     */
    public String getMethodName() {
        return methodName;
    }

    public MethodInterceptorFilter getMethodFilter() {
        return methodFilter;
    }
}
