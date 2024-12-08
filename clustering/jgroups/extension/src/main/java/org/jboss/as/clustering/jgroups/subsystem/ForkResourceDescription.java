/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.controller.ResourceDescription;
import org.jboss.as.controller.PathElement;

/**
 * Description of a fork channel resource.
 * @author Paul Ferraro
 */
public enum ForkResourceDescription implements ResourceDescription {
    INSTANCE;

    private static final PathElement PATH = pathElement(PathElement.WILDCARD_VALUE);
    static PathElement pathElement(String name) {
        return PathElement.pathElement("fork", name);
    }

    @Override
    public PathElement getPathElement() {
        return PATH;
    }
}
