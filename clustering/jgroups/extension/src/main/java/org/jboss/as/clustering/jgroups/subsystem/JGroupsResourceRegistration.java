/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;

/**
 * Enumeration of primary JGroups resource registrations.
 * @author Paul Ferraro
 */
public enum JGroupsResourceRegistration implements ResourceRegistration {
    STACK("stack"),
    CHANNEL("channel"),
    FORK("fork"),
    REMOTE_SITE("remote-site")
    ;
    private final PathElement path;

    JGroupsResourceRegistration(String name) {
        this.path = PathElement.pathElement(name);
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    public PathElement pathElement(String value) {
        return PathElement.pathElement(this.path.getKey(), value);
    }
}
