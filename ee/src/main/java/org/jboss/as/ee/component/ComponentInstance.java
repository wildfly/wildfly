/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import org.jboss.invocation.Interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * An instance of a Jakarta EE component.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ComponentInstance extends Serializable {

    /**
     * Get the component associated with this instance.
     *
     * @return the component
     */
    Component getComponent();

    /**
     * Get the actual object instance.  The object instance has all injections filled.
     *
     * @return the instance
     */
    Object getInstance();

    /**
     * Get the instance interceptor (entry point) for the given method.  This is the internal entry point for
     * the component instance, which bypasses view interceptors.
     *
     * @param method the method
     * @return the interceptor
     * @throws IllegalStateException if the method does not exist
     */
    Interceptor getInterceptor(Method method) throws IllegalStateException;

    /**
     * Get the list of allowed methods for this component instance.  The handler will only accept invocations on
     * these exact {@code Method} objects.
     *
     * @return the list of methods
     */
    Collection<Method> allowedMethods();

    /**
     * Destroy this component instance.  Implementations of this method must be idempotent, meaning that destroying
     * a component instance more than one time has no additional effect.
     */
    void destroy();

    /**
     * Gets some data that was attached to this component instance
     *
     * @param key The component data key
     * @return The attached data
     */
    Object getInstanceData(final Object key);

    /**
     * Attaches some data to this component instance. This should only be used during component construction.
     *
     * @param key The key to store the data
     * @param value The data value
     */
    void setInstanceData(final Object key, final Object value);

}
