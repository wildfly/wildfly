/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.web;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;

/**
 * Enumerates affinity resource registrations.
 * @author Paul Ferraro
 */
public enum AffinityResourceRegistration implements ResourceRegistration {
    WILDCARD(PathElement.WILDCARD_VALUE),
    NONE("none"),
    LOCAL("local"),
    PRIMARY_OWNER("primary-owner"),
    RANKED("ranked"),
    ;
    private final PathElement path;

    AffinityResourceRegistration(String value) {
        this.path = PathElement.pathElement("affinity", value);
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }
}
