/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.pool;

/**
 * Creates and destroys stateless objects.
 * <p/>
 * The object returned by create has dependencies injected. The PostConstruct
 * callback, if defined, has been called.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public interface StatelessObjectFactory<T> {
    /**
     * Creates a new stateless object by calling it's empty constructor,
     * do injection and calling post-construct.
     *
     * @return
     */
    T create();

    /**
     * Perform any cleanup actions on the object, such as
     * calling the pre-destroy callback.
     *
     * @param obj the object
     */
    void destroy(T obj);
}
