/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.timerservice.spi;

import java.lang.reflect.Method;

import jakarta.ejb.Timer;

import org.jboss.as.ejb3.component.EJBComponent;

/**
 * An implementation can invoke the ejbTimeout method on a TimedObject.
 * <p/>
 * The TimedObjectInvoker has knowledge of the TimedObjectId, it
 * knows which object to invoke.
 *
 * @author Thomas.Diesler@jboss.org
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public interface TimedObjectInvoker {

    /**
     * Return the EJB component associated with this invoker
     * @return an EJB component
     */
    EJBComponent getComponent();

    /**
     * The globally unique identifier for this timed object invoker.
     *
     * @return the identifier
     */
    String getTimedObjectId();

    /**
     * Invokes the ejbTimeout method on the TimedObject with the given id.
     *
     * @param timer the Timer that is passed to ejbTimeout
     */
    default void callTimeout(Timer timer) throws Exception {
        this.callTimeout(timer, this.getComponent().getTimeoutMethod());
    }

    /**
     * Responsible for invoking the timeout method on the target object.
     * <p/>
     * <p>
     * The timerservice implementation invokes this method as a callback when a timeout occurs for the passed
     * <code>timer</code>. The timerservice implementation will be responsible for passing the correct
     * timeout method corresponding to the <code>timer</code> on which the timeout has occurred.
     * </p>
     *
     * @param timer         the Timer that is passed to ejbTimeout
     * @param timeoutMethod The timeout method
     */
    void callTimeout(Timer timer, Method timeoutMethod) throws Exception;

    /**
     * @return The class loader that should be used to load restore any timers for this object
     */
    ClassLoader getClassLoader();
}
