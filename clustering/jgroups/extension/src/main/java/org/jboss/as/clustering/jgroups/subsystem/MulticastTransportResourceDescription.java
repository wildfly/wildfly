/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.PathElement;

/**
 * Descriptions for multicast transport resources.
 * @author Paul Ferraro
 */
public enum MulticastTransportResourceDescription implements TransportResourceDescription {
    UDP;

    private final PathElement path = TransportResourceDescription.pathElement(this.name());

    @Override
    public PathElement getPathElement() {
        return this.path;
    }
}
