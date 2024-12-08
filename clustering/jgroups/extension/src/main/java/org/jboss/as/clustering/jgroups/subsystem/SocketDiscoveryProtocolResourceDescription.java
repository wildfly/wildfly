/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.net.InetSocketAddress;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.network.OutboundSocketBinding;
import org.jgroups.PhysicalAddress;
import org.jgroups.stack.IpAddress;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceListAttributeDefinition;

/**
 * Descriptions of socket discovery protocol resources.
 * @author Paul Ferraro
 */
public enum SocketDiscoveryProtocolResourceDescription implements ProtocolResourceDescription {
    TCPGOSSIP(InetSocketAddress.class, Function.identity()),
    TCPPING(PhysicalAddress.class, address -> new IpAddress(address.getAddress(), address.getPort())),
    ;

    static final CapabilityReferenceListAttributeDefinition<OutboundSocketBinding> OUTBOUND_SOCKET_BINDINGS = new CapabilityReferenceListAttributeDefinition.Builder<>("socket-bindings", CapabilityReference.builder(CAPABILITY, OutboundSocketBinding.SERVICE_DESCRIPTOR).build())
            .setMinSize(1)
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    private final PathElement path = ProtocolResourceDescription.pathElement(this.name());
    private final Function<InetSocketAddress, ?> hostTransformer;

    <A> SocketDiscoveryProtocolResourceDescription(Class<A> hostClass, Function<InetSocketAddress, A> hostTransformer) {
        this.hostTransformer = hostTransformer;
    }

    Function<InetSocketAddress, ?> getHostTransformer() {
        return this.hostTransformer;
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(Stream.of(OUTBOUND_SOCKET_BINDINGS), ProtocolResourceDescription.super.getAttributes());
    }
}
