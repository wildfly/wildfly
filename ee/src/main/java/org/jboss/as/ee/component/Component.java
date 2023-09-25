/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import org.jboss.as.naming.context.NamespaceContextSelector;

/**
 * Common contract for an EE component.  Implementations of this will be available as a service and can be used as the
 * backing for a JNDI ObjectFactory reference.
 *
 * @author John Bailey
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface Component {

    /**
     * Start operation called when the Component is available.
     */
    void start();

    /**
     * Stop operation called when the Component is no longer available.
     *
     */
    void stop();

    /**
     * Get the component's actual implementation class.
     *
     * @return the component class
     */
    Class<?> getComponentClass();

    /**
     * Create a new instance of this component.  This may be invoked by a component interceptor, a client interceptor,
     * or in the course of creating a new client, or in the case of an "eager" singleton, at component start.  This
     * method will block until the component is available.  If the component fails to start then a runtime exception
     * will be thrown.
     *
     * @return the component instance
     */
    ComponentInstance createInstance();

    ComponentInstance createInstance(Object instance);

    /**
     * Returns a component instance for a pre-existing instance.
     * @param instance the actual object instance
     * @return a component instance
     */
    ComponentInstance getInstance(Object instance);

    NamespaceContextSelector getNamespaceContextSelector();

    /**
     * Checks whether the supplied {@link Throwable} is remotable meaning it can be safely sent to the client over the wire.
     */
    default boolean isRemotable(Throwable throwable) {
        return true;
    }

    void waitForComponentStart();

}
