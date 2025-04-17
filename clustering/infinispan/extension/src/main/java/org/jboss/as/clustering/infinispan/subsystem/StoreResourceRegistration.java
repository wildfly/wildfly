/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;

/**
 * Enumerates resource registrations for cache stores.
 * @author Paul Ferraro
 */
public enum StoreResourceRegistration implements ResourceRegistration {
    WILDCARD(PathElement.WILDCARD_VALUE),
    NONE("none"),
    CUSTOM("custom"),
    FILE("file"),
    HOTROD("hotrod"),
    JDBC("jdbc"),
    REMOTE("remote"),
    ;
    private final PathElement path;

    StoreResourceRegistration(String value) {
        this.path = PathElement.pathElement("store", value);
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }
}
