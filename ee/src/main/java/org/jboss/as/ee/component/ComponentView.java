/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
