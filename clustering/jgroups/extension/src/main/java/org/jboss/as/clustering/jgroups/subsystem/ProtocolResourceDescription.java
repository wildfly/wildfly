/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jgroups.Global;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;

/**
 * Describes a protocol resource.
 * @author Paul Ferraro
 */
public interface ProtocolResourceDescription extends ProtocolChildResourceDescription {
    ProtocolResourceDescription INSTANCE = of(PathElement.WILDCARD_VALUE);

    RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(ProtocolConfiguration.SERVICE_DESCRIPTOR).setAllowMultipleRegistrations(true).build();

    static PathElement pathElement(String name) {
        return PathElement.pathElement("protocol", name);
    }

    static PathElement legacyPathElement(String name) {
        return pathElement(Global.PREFIX + name);
    }

    static ProtocolResourceDescription of(String name) {
        PathElement path = pathElement(name);
        return new ProtocolResourceDescription() {
            @Override
            public PathElement getPathElement() {
                return path;
            }
        };
    }

    static ProtocolResourceDescription legacy(PathElement path) {
        return of(Global.PREFIX + path.getValue());
    }

    static ProtocolResourceDescription legacy(ProtocolResourceDescription description) {
        return legacy(description.getPathElement());
    }
}
