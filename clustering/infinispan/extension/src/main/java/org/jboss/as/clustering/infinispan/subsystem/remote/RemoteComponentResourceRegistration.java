/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem.remote;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;

/**
 * Enumerates resource registrations for components of a remote cache container.
 * @author Paul Ferraro
 */
public enum RemoteComponentResourceRegistration implements ResourceRegistration {
    CONNECTION_POOL("connection-pool"),
    SECURITY("security"),
    ;
    private final PathElement path;

    RemoteComponentResourceRegistration(String value) {
        this.path = PathElement.pathElement("component", value);
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }
}
