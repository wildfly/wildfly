/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.stream.Stream;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.network.SocketBinding;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;

/**
 * @author Paul Ferraro
 */
public enum MulticastProtocolResourceDescription implements ProtocolResourceDescription {
    MPING
    ;

    static final CapabilityReferenceAttributeDefinition<SocketBinding> SOCKET_BINDING = new CapabilityReferenceAttributeDefinition.Builder<>(ModelDescriptionConstants.SOCKET_BINDING, CapabilityReference.builder(CAPABILITY, SocketBinding.SERVICE_DESCRIPTOR).build())
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    private final PathElement path = ProtocolResourceDescription.pathElement(this.name());

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(Stream.of(SOCKET_BINDING), ProtocolResourceDescription.super.getAttributes());
    }
}
