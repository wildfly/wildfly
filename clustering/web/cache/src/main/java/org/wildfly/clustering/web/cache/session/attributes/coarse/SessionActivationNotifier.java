/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.attributes.coarse;

/**
 * Notifies attributes of a session implementing session activation listener.
 * @author Paul Ferraro
 */
public interface SessionActivationNotifier {

    /**
     * Notifies interested attributes that they will be passivated.
     */
    void prePassivate();

    /**
     * Notifies interested attributes that they are were activated.
     */
    void postActivate();
}
