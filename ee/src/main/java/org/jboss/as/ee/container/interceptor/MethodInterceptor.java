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

package org.jboss.as.ee.container.interceptor;

/**
 * Contract for intercepting a method.
 *
 * @author John Bailey
 */
public interface MethodInterceptor {
    /**
     * Get the method filter used to determine whether or not to apply this filter against a method.
     *
     * @return The method filter
     */
    MethodInterceptorFilter getMethodFilter();

    /**
     * Determine whether to use an {@link InvocationContext} for interception methods.
     *
     * @return {@code true} if this interceptor accepts an InvocationContext
     */
    boolean acceptsInvocationContext();

    /**
     * Intercept a method call without an InvocationContext.
     *
     * @param target The object being intercepted
     * @return The result of the method call
     * @throws Exception If any exceptions occur during interception
     */
    Object intercept(final Object target) throws Exception;

    /**
     * Intercept a method call with an InvocationContext..
     *
     * @param target            The object being intercepted
     * @param invocationContext The current InvocationContext
     * @return The result of the method call
     * @throws Exception If any exceptions occur during interception
     */
    Object intercept(final Object target, final InvocationContext<?> invocationContext) throws Exception;
}
