/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.PathElement;

/**
 * @author Paul Ferraro
 */
public enum AuthProtocolResourceDescription implements ProtocolResourceDescription {
    AUTH
    ;
    private final PathElement path = ProtocolResourceDescription.pathElement(this.name());

    @Override
    public PathElement getPathElement() {
        return this.path;
    }
}
