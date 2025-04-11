/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;

import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jgroups.protocols.TP;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

/**
 * Registers the resource definitions for all supported transport protocols.
 * @author Paul Ferraro
 */
public class TransportResourceDefinitionRegistrar extends AbstractTransportResourceDefinitionRegistrar<TP> {

    private final ResourceOperationRuntimeHandler parentRuntimeHandler;

    TransportResourceDefinitionRegistrar(ResourceOperationRuntimeHandler parentRuntimeHandler) {
        super(new Configurator() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return StackResourceDefinitionRegistrar.Component.TRANSPORT;
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

        // Register transport override definitions
        for (MulticastTransportResourceDefinitionRegistrar.Transport transport : EnumSet.allOf(MulticastTransportResourceDefinitionRegistrar.Transport.class)) {
            new MulticastTransportResourceDefinitionRegistrar(transport, this.parentRuntimeHandler).register(parent, context);
        }
        for (SocketTransportResourceDefinitionRegistrar.Transport transport : EnumSet.allOf(SocketTransportResourceDefinitionRegistrar.Transport.class)) {
            new SocketTransportResourceDefinitionRegistrar<>(transport, this.parentRuntimeHandler).register(parent, context);
        }

        // Register children
        for (ThreadPoolResourceDefinitionRegistrar pool : EnumSet.allOf(ThreadPoolResourceDefinitionRegistrar.class)) {
            pool.register(registration, context);
        }

        return registration;
    }
}
