/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

/**
 * Records some other object.
 * @author Paul Ferraro
 */
public interface Recordable<T> {
    /**
     * Records the specified object
     * @param object an object to record
     */
    void record(T object);

    /**
     * Resets any previously recorded objects
     */
    void reset();
}
