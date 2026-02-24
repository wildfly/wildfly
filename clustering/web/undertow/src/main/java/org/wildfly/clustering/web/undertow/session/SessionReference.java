/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import org.wildfly.clustering.server.util.Reference;

import io.undertow.server.session.SessionManager;

/**
 * A reference to an Undertow session.
 * @author Paul Ferraro
 */
// TODO Replace with io.undertow.server.session.SessionReference from Undertow 2.4.x
public interface SessionReference extends Reference<io.undertow.server.session.Session> {

    /**
     * Returns the manager associated with the referenced session.
     * @return the manager associated with the referenced session.
     */
    SessionManager getManager();

    /**
     * Returns the identifier of the referenced session.
     * @return the identifier of the referenced session.
     */
    String getId();
}
