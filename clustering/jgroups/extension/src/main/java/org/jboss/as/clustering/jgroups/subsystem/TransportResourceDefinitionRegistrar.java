/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;
import java.util.stream.Stream;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jgroups.protocols.TP;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

/**
 * Registers the resource definitions for all supported transport protocols.
 * @author Paul Ferraro
 */
public class TransportResourceDefinitionRegistrar extends AbstractTransportResourceDefinitionRegistrar<TP> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static Stream<AttributeDefinition> attributes() {
        return AbstractTransportResourceDefinitionRegistrar.attributes();
    }

    enum MulticastTransport {
        UDP;
    }

    enum SocketTransport {
        TCP, TCP_NIO2;
    }

    private final ResourceOperationRuntimeHandler parentRuntimeHandler;

    TransportResourceDefinitionRegistrar(ResourceOperationRuntimeHandler parentRuntimeHandler) {
        super(new TransportResourceRegistration<>() {
            @Override
            public PathElement getPathElement() {
                return WILDCARD_PATH;
            }

            @Override
            public ResourceOperationRuntimeHandler getParentRuntimeHandler() {
                return parentRuntimeHandler;
            }
        });
        this.parentRuntimeHandler = parentRuntimeHandler;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);
        for (ThreadPoolResourceDefinitionRegistrar pool : EnumSet.allOf(ThreadPoolResourceDefinitionRegistrar.class)) {
            pool.register(registration, context);
        }

        for (MulticastTransport transport : EnumSet.allOf(MulticastTransport.class)) {
            new MulticastTransportResourceDefinitionRegistrar(transport.name(), this.parentRuntimeHandler).register(parent, context);
        }

        for (SocketTransport transport : EnumSet.allOf(SocketTransport.class)) {
            new SocketTransportResourceDefinitionRegistrar<>(transport.name(), this.parentRuntimeHandler).register(parent, context);
        }

        return registration;
    }
}
