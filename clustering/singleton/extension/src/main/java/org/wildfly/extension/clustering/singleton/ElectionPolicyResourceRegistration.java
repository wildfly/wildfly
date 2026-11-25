/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.singleton;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;

/**
 * Enumerates resource registration for election policies.
 * @author Paul Ferraro
 */
public enum ElectionPolicyResourceRegistration implements ResourceRegistration {
    WILDCARD(PathElement.WILDCARD_VALUE),
    SIMPLE("simple"),
    RANDOM("random"),
    ;
    private final PathElement path;

    ElectionPolicyResourceRegistration(String value) {
        this.path = PathElement.pathElement("election-policy", value);
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }
}
