/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache;

/**
 * Factory for creating stateful session bean instances.
 * @author Paul Ferraro
 * @param <V> the bean instance type
 */
public interface StatefulSessionBeanInstanceFactory<V> {
    /**
     * Create a new instance of this component.  This may be invoked by a component interceptor, a client interceptor,
     * or in the course of creating a new client, or in the case of an "eager" singleton, at component start.  This
     * method will block until the component is available.  If the component fails to start then a runtime exception
     * will be thrown.
     * <p/>
     * The instance has been injected and post-construct has been called.
     * @return the component instance
     */
    V createInstance();
}
