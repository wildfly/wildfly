/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session;

/**
 * Notifies attributes of a session implementing session binding listener.
 * @author Paul Ferraro
 */
public interface SessionBindingNotifier {

    /**
     * Notifies all attributes that they are being unbound from a given session.
     */
    void bound();

    /**
     * Notifies all attributes that they are being unbound from a given session.
     */
    void unbound();
}
