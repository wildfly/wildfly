/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;

import org.jboss.as.controller.ResourceRegistration;
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
        super(new Configurator() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return StackResourceDefinitionRegistrar.Component.PROTOCOL;
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
        for (SocketProtocolResourceRegistration protocol : EnumSet.allOf(SocketProtocolResourceRegistration.class)) {
            protocol.apply(this.parentRuntimeHandler).register(parent, context);
        }

        for (MulticastProtocolResourceDefinitionRegistrar.Protocol protocol : EnumSet.allOf(MulticastProtocolResourceDefinitionRegistrar.Protocol.class)) {
            new MulticastProtocolResourceDefinitionRegistrar(protocol, this.parentRuntimeHandler).register(parent, context);
        }

        for (JDBCProtocolResourceDefinitionRegistrar.Protocol protocol : EnumSet.allOf(JDBCProtocolResourceDefinitionRegistrar.Protocol.class)) {
            new JDBCProtocolResourceDefinitionRegistrar(protocol, this.parentRuntimeHandler).register(parent, context);
            // Add deprecated override definition for legacy variant
            new NativeProtocolResourceDefinitionRegistrar<>(protocol, JGroupsSubsystemModel.VERSION_5_0_0, this.parentRuntimeHandler).register(parent, context);
        }

        for (EncryptProtocolResourceDefinitionRegistrar.Protocol protocol : EnumSet.allOf(EncryptProtocolResourceDefinitionRegistrar.Protocol.class)) {
            new EncryptProtocolResourceDefinitionRegistrar<>(protocol, protocol.getKeyStoreEntryClass(), this.parentRuntimeHandler).register(parent, context);
            // Add deprecated override definition for legacy variant
            new NativeProtocolResourceDefinitionRegistrar<>(protocol, JGroupsSubsystemModel.VERSION_5_0_0, this.parentRuntimeHandler).register(parent, context);
        }

        for (SocketDiscoveryProtocolResourceDefinitionRegistrar.Protocol protocol : EnumSet.allOf(SocketDiscoveryProtocolResourceDefinitionRegistrar.Protocol.class)) {
            new SocketDiscoveryProtocolResourceDefinitionRegistrar<>(protocol, protocol.getHostTransformer(), this.parentRuntimeHandler).register(parent, context);
            // Add deprecated override definition for legacy variant
            new NativeProtocolResourceDefinitionRegistrar<>(protocol, JGroupsSubsystemModel.VERSION_5_0_0, this.parentRuntimeHandler).register(parent, context);
        }

        for (AuthProtocolResourceDefinitionRegistrar.Protocol protocol : EnumSet.allOf(AuthProtocolResourceDefinitionRegistrar.Protocol.class)) {
            new AuthProtocolResourceDefinitionRegistrar(protocol, this.parentRuntimeHandler).register(parent, context);
            // Add deprecated override definition for legacy variant
            new NativeProtocolResourceDefinitionRegistrar<>(protocol, JGroupsSubsystemModel.VERSION_5_0_0, this.parentRuntimeHandler).register(parent, context);
        }

        // only auto-update legacy protocols in server processes
        if (parent.getProcessType().isServer()) {
            for (LegacyProtocolResourceDefinitionRegistrar.Protocol protocol : EnumSet.allOf(LegacyProtocolResourceDefinitionRegistrar.Protocol.class)) {
                new LegacyProtocolResourceDefinitionRegistrar(protocol).register(parent, context);
            }
        }

        return registration;
    }
}
