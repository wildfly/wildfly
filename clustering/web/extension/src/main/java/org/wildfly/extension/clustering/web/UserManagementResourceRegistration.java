/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.web;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;

/**
 * Enumerates user management resource registrations.
 * @author Paul Ferraro
 */
public enum UserManagementResourceRegistration implements ResourceRegistration {
    INFINISPAN("infinispan-single-sign-on-management"),
    HOTROD("hotrod-single-sign-on-management"),
    ;
    private final PathElement path;

    UserManagementResourceRegistration(String key) {
        this.path = PathElement.pathElement(key);
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }
}
