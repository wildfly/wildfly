/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;

/**
 * Enumerates bean management resource registrations.
 * @author Paul Ferraro
 */
public enum BeanManagementResourceRegistration implements ResourceRegistration {
    INFINISPAN("infinispan-bean-management"),
    ;
    private final PathElement path;

    BeanManagementResourceRegistration(String key) {
        this.path = PathElement.pathElement(key);
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    PathElement pathElement(String value) {
        return PathElement.pathElement(this.path.getKey(), value);
    }
}
