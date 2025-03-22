/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.web;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;

/**
 * Enumerates routing provider resource registrations.
 * @author Paul Ferraro
 */
public enum RoutingProviderResourceRegistration implements ResourceRegistration {
    WILDCARD(PathElement.WILDCARD_VALUE),
    LOCAL("local"),
    INFINISPAN("infinispan"),
    ;
    private final PathElement path;

    RoutingProviderResourceRegistration(String value) {
        this.path = PathElement.pathElement("routing", value);
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }
}
