/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;

import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jgroups.stack.Protocol;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

/**
 * Registers resource definitions for all supported JGroups protocols.
 * @author Paul Ferraro
 */
public class ProtocolResourceDefinitionRegistrar extends AbstractProtocolResourceDefinitionRegistrar<Protocol> {

    private final ResourceOperationRuntimeHandler parentRuntimeHandler;

    ProtocolResourceDefinitionRegistrar(ResourceOperationRuntimeHandler parentRuntimeHandler) {
        super(new ProtocolResourceDescriptorConfigurator<>() {
            @Override
            public ProtocolResourceDescription getResourceDescription() {
                return ProtocolResourceDescription.INSTANCE;
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

        // Register protocol override definitions
        for (SocketProtocolResourceDescription protocol : EnumSet.allOf(SocketProtocolResourceDescription.class)) {
            protocol.apply(this.parentRuntimeHandler).register(parent, context);
        }

        for (MulticastProtocolResourceDescription protocol : EnumSet.allOf(MulticastProtocolResourceDescription.class)) {
            new MulticastProtocolResourceDefinitionRegistrar(protocol, this.parentRuntimeHandler).register(parent, context);
        }

        for (JDBCProtocolResourceDescription protocol : EnumSet.allOf(JDBCProtocolResourceDescription.class)) {
            new JDBCProtocolResourceDefinitionRegistrar(protocol, this.parentRuntimeHandler).register(parent, context);
            // Add deprecated override definition for legacy variant
            new NativeProtocolResourceDefinitionRegistrar<>(protocol, JGroupsSubsystemModel.VERSION_5_0_0, this.parentRuntimeHandler).register(parent, context);
        }

        for (EncryptProtocolResourceDescription protocol : EnumSet.allOf(EncryptProtocolResourceDescription.class)) {
            new EncryptProtocolResourceDefinitionRegistrar<>(protocol, protocol.getKeyStoreEntryClass(), this.parentRuntimeHandler).register(parent, context);
            // Add deprecated override definition for legacy variant
            new NativeProtocolResourceDefinitionRegistrar<>(protocol, JGroupsSubsystemModel.VERSION_5_0_0, this.parentRuntimeHandler).register(parent, context);
        }

        for (SocketDiscoveryProtocolResourceDescription protocol : EnumSet.allOf(SocketDiscoveryProtocolResourceDescription.class)) {
            new SocketDiscoveryProtocolResourceDefinitionRegistrar<>(protocol, protocol.getHostTransformer(), this.parentRuntimeHandler).register(parent, context);
            // Add deprecated override definition for legacy variant
            new NativeProtocolResourceDefinitionRegistrar<>(protocol, JGroupsSubsystemModel.VERSION_5_0_0, this.parentRuntimeHandler).register(parent, context);
        }

        for (AuthProtocolResourceDescription protocol : EnumSet.allOf(AuthProtocolResourceDescription.class)) {
            new AuthProtocolResourceDefinitionRegistrar(protocol, this.parentRuntimeHandler).register(parent, context);
            // Add deprecated override definition for legacy variant
            new NativeProtocolResourceDefinitionRegistrar<>(protocol, JGroupsSubsystemModel.VERSION_5_0_0, this.parentRuntimeHandler).register(parent, context);
        }

        // only auto-update legacy protocols in server processes
        if (parent.getProcessType().isServer()) {
            for (LegacyProtocolResourceDescription protocol : EnumSet.allOf(LegacyProtocolResourceDescription.class)) {
                new LegacyProtocolResourceDefinitionRegistrar(protocol).register(parent, context);
            }
        }

        return registration;
    }
}
