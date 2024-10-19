/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;

/**
 * Enumerates the resource registrations of an async store component.
 * @author Paul Ferraro
 */
public enum StoreWriteResourceRegistration implements ResourceRegistration {
    WILDCARD(PathElement.WILDCARD_VALUE),
    BEHIND("behind"),
    THROUGH("through"),
    ;
    private final PathElement path;

    StoreWriteResourceRegistration(String value) {
        this.path = PathElement.pathElement("write", value);
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }
}
