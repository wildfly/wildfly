/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.PathElement;
import org.jgroups.Global;
import org.jgroups.protocols.FD_ALL2;
import org.jgroups.stack.Protocol;

/**
 * @author Paul Ferraro
 *
 */
public enum LegacyProtocolResourceDescription implements ProtocolResourceDescription {
    FD(FD_ALL2.class, JGroupsSubsystemModel.VERSION_10_0_0),
    ;
    private final PathElement path;
    private final PathElement targetPath;
    private final JGroupsSubsystemModel deprecation;

    LegacyProtocolResourceDescription(Class<? extends Protocol> targetProtocol, JGroupsSubsystemModel deprecation) {
        this(null, targetProtocol, deprecation);
    }

    LegacyProtocolResourceDescription(String name, Class<? extends Protocol> targetProtocol, JGroupsSubsystemModel deprecation) {
        this.path = ProtocolResourceDescription.pathElement((name != null) ? name : this.name());
        this.targetPath = ProtocolResourceDescription.pathElement(targetProtocol.getName().substring(Global.PREFIX.length()));
        this.deprecation = deprecation;
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    PathElement getTargetPathElement() {
        return this.targetPath;
    }

    JGroupsSubsystemModel getDeprecation() {
        return this.deprecation;
    }
}
