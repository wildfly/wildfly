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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.InterceptorContext;

/**
 * A component view.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ComponentView {

    /**
     * Create the component view instance.
     *
     * @return the component view instance
     */
    ManagedReference createInstance() throws Exception;

    /**
     * Create the component view instance using the additional context data
     *
     * @param contextData Additional context data used in the view creation
     * @return the component view instance
     */
    ManagedReference createInstance(Map<Object, Object> contextData) throws Exception;

    /**
     * Invoke on the component view interceptor chain.
     * TODO: fully document the semantics of this method
     *
     * @param interceptorContext The context of the invocation
     * @return The result of the invocation
     */
    Object invoke(final InterceptorContext interceptorContext) throws Exception;

    /**
     * Get the associated component.
     *
     * @return the component
     */
    Component getComponent();

    /**
     *
     * @return The proxy class used in the view
     */
    Class<?> getProxyClass();

    /**
     *
     * @return The class of the view
     */
    Class<?> getViewClass();

    /**
     *
     * @return All methods that the view supports
     */
    Set<Method> getViewMethods();

    /**
     * Gets a view method based on name and descriptor
     * @param name the method name
     * @param descriptor The method descriptor in JVM format
     * @return The method that corresponds to the given name and descriptor
     * @throws IllegalArgumentException If the method cannot be found
     */
    Method getMethod(final String name, final String descriptor);

    /**
     * Provides a mechanism to attach arbitrary data to the component view
     * @param clazz The class of attachment
     * @return The data, or null if it is not present
     */
    <T> T getPrivateData(Class<T> clazz);


    boolean isAsynchronous(final Method method);

}
