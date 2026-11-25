/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;

/**
 * Enumerates the transport resource registrations of a cache container.
 * @author Paul Ferraro
 */
public enum TransportResourceRegistration implements ResourceRegistration {
    WILDCARD(PathElement.WILDCARD_VALUE),
    JGROUPS("jgroups"),
    NONE("none"),
    ;
    private final PathElement path;

    TransportResourceRegistration(String value) {
        this.path = PathElement.pathElement("transport", value);
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }
}
