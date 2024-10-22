/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jgroups.Global;
import org.jgroups.PhysicalAddress;
import org.jgroups.protocols.FD_ALL2;
import org.jgroups.stack.IpAddress;
import org.jgroups.stack.Protocol;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

/**
 * Registers resource definitions for all supported JGroups protocols.
 * @author Paul Ferraro
 */
public class ProtocolResourceDefinitionRegistrar extends AbstractProtocolResourceDefinitionRegistrar<Protocol> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    enum AuthProtocol {
        AUTH;
    }

    enum EncryptProtocol {
        ASYM_ENCRYPT(KeyStore.PrivateKeyEntry.class),
        SYM_ENCRYPT(KeyStore.SecretKeyEntry.class),
        ;
        final Class<? extends KeyStore.Entry> entryClass;

        EncryptProtocol(Class<? extends KeyStore.Entry> entryClass) {
            this.entryClass = entryClass;
        }
    }

    enum InitialHostsProtocol {
        TCPGOSSIP(InetSocketAddress.class, Function.identity()),
        TCPPING(PhysicalAddress.class, address -> new IpAddress(address.getAddress(), address.getPort())),
        ;
        final Function<InetSocketAddress, ?> hostTransformer;

        <A> InitialHostsProtocol(Class<A> hostClass, Function<InetSocketAddress, A> hostTransformer) {
            this.hostTransformer = hostTransformer;
        }
    }

    enum JdbcProtocol {
        JDBC_PING;
    }

    enum MulticastProtocol {
        MPING;
    }

    enum SocketProtocol implements Function<ResourceOperationRuntimeHandler, ChildResourceDefinitionRegistrar> {
        FD_SOCK() {
            @Override
            public ChildResourceDefinitionRegistrar apply(ResourceOperationRuntimeHandler runtimeHandler) {
                return new LegacyFailureDetectionProtocolResourceDefinitionRegistrar(this.name(), runtimeHandler);
            }
        },
        FD_SOCK2() {
            @Override
            public ChildResourceDefinitionRegistrar apply(ResourceOperationRuntimeHandler runtimeHandler) {
                return new FailureDetectionProtocolResourceDefinitionRegistrar(this.name(), runtimeHandler);
            }
        },
        ;
    }

    enum LegacyProtocol {
        FD(FD_ALL2.class, JGroupsSubsystemModel.VERSION_10_0_0),
        ;
        final String name;
        final String targetName;
        final JGroupsSubsystemModel deprecation;

        LegacyProtocol(Class<? extends Protocol> targetProtocol, JGroupsSubsystemModel deprecation) {
            this(null, targetProtocol, deprecation);
        }

        LegacyProtocol(String name, Class<? extends Protocol> targetProtocol, JGroupsSubsystemModel deprecation) {
            this.name = (name != null) ? name : this.name();
            this.targetName = targetProtocol.getName().substring(Global.PREFIX.length());
            this.deprecation = deprecation;
        }
    }

    static Stream<AttributeDefinition> attributes() {
        return AbstractProtocolResourceDefinitionRegistrar.attributes();
    }

    private final ResourceOperationRuntimeHandler parentRuntimeHandler;

    ProtocolResourceDefinitionRegistrar(ResourceOperationRuntimeHandler parentRuntimeHandler) {
        super(new ProtocolResourceRegistration<>() {
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

        // Override definitions for protocol types
        for (SocketProtocol protocol : EnumSet.allOf(SocketProtocol.class)) {
            protocol.apply(this.parentRuntimeHandler).register(parent, context);
        }
        for (MulticastProtocol protocol : EnumSet.allOf(MulticastProtocol.class)) {
            new MulticastProtocolResourceDefinitionRegistrar(protocol.name(), this.parentRuntimeHandler).register(parent, context);
        }

        for (JdbcProtocol protocol : EnumSet.allOf(JdbcProtocol.class)) {
            new JDBCProtocolResourceDefinitionRegistrar(protocol.name(), this.parentRuntimeHandler).register(parent, context);
            // Add deprecated override definition for legacy variant
            new NativeProtocolResourceDefinitionRegistrar<>(protocol.name(), JGroupsSubsystemModel.VERSION_5_0_0, this.parentRuntimeHandler).register(parent, context);
        }

        for (EncryptProtocol protocol : EnumSet.allOf(EncryptProtocol.class)) {
            new EncryptProtocolResourceDefinitionRegistrar<>(protocol.name(), protocol.entryClass, this.parentRuntimeHandler).register(parent, context);
            // Add deprecated override definition for legacy variant
            new NativeProtocolResourceDefinitionRegistrar<>(protocol.name(), JGroupsSubsystemModel.VERSION_5_0_0, this.parentRuntimeHandler).register(parent, context);
        }

        for (InitialHostsProtocol protocol : EnumSet.allOf(InitialHostsProtocol.class)) {
            new SocketDiscoveryProtocolResourceDefinitionRegistrar<>(protocol.name(), protocol.hostTransformer, this.parentRuntimeHandler).register(parent, context);
            // Add deprecated override definition for legacy variant
            new NativeProtocolResourceDefinitionRegistrar<>(protocol.name(), JGroupsSubsystemModel.VERSION_5_0_0, this.parentRuntimeHandler).register(parent, context);
        }

        for (AuthProtocol protocol : EnumSet.allOf(AuthProtocol.class)) {
            new AuthProtocolResourceDefinitionRegistrar(protocol.name(), this.parentRuntimeHandler).register(parent, context);
            // Add deprecated override definition for legacy variant
            new NativeProtocolResourceDefinitionRegistrar<>(protocol.name(), JGroupsSubsystemModel.VERSION_5_0_0, this.parentRuntimeHandler).register(parent, context);
        }

        // only auto-update legacy protocols in server processes
        if (parent.getProcessType().isServer()) {
            for (LegacyProtocol protocol : EnumSet.allOf(LegacyProtocol.class)) {
                new LegacyProtocolResourceDefinitionRegistrar(protocol.name, protocol.targetName, protocol.deprecation).register(parent, context);
            }
        }

        return registration;
    }
}
