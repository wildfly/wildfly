/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ManagementRegistrar;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.RuntimeResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jgroups.Global;
import org.jgroups.PhysicalAddress;
import org.jgroups.protocols.FD_ALL2;
import org.jgroups.stack.IpAddress;
import org.jgroups.stack.Protocol;

/**
 * Registers protocol resource definitions, including any overrides.
 * @author Paul Ferraro
 */
public class ProtocolResourceRegistrar implements ManagementRegistrar<ManagementResourceRegistration> {

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

    enum SocketProtocol {
        FD_SOCK(LegacyFailureDetectionProtocolConfigurationServiceConfigurator::new),
        FD_SOCK2(FailureDetectionProtocolConfigurationServiceConfigurator::new),
        ;
        private final ResourceServiceConfiguratorFactory factory;

        SocketProtocol(ResourceServiceConfiguratorFactory factory) {
            this.factory = factory;
        }

        ResourceServiceConfiguratorFactory getResourceServiceConfiguratorFactory() {
            return this.factory;
        }
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

    private static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        private final RuntimeResourceRegistration runtimeResourceRegistration;

        ResourceDescriptorConfigurator(RuntimeResourceRegistration runtimeResourceRegistration) {
            this.runtimeResourceRegistration = runtimeResourceRegistration;
        }

        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return descriptor.addRuntimeResourceRegistration(this.runtimeResourceRegistration);
        }
    }

    private final ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory;
    private final UnaryOperator<ResourceDescriptor> configurator;

    ProtocolResourceRegistrar(ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        this.parentServiceConfiguratorFactory = parentServiceConfiguratorFactory;
        this.configurator = UnaryOperator.identity();
    }

    ProtocolResourceRegistrar(ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory, RuntimeResourceRegistration runtimeResourceRegistration) {
        this.parentServiceConfiguratorFactory = parentServiceConfiguratorFactory;
        this.configurator = new ResourceDescriptorConfigurator(runtimeResourceRegistration);
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        new GenericProtocolResourceDefinition(this.configurator, this.parentServiceConfiguratorFactory).register(registration);

        // Override definitions for protocol types
        for (SocketProtocol protocol : EnumSet.allOf(SocketProtocol.class)) {
            new SocketProtocolResourceDefinition(protocol.name(), this.configurator, protocol.getResourceServiceConfiguratorFactory(), this.parentServiceConfiguratorFactory).register(registration);
        }
        for (MulticastProtocol protocol : EnumSet.allOf(MulticastProtocol.class)) {
            new MulticastProtocolResourceDefinition(protocol.name(), this.configurator, this.parentServiceConfiguratorFactory).register(registration);
        }

        for (JdbcProtocol protocol : EnumSet.allOf(JdbcProtocol.class)) {
            new JDBCProtocolResourceDefinition(protocol.name(), this.configurator, this.parentServiceConfiguratorFactory).register(registration);
            // Add deprecated override definition for legacy variant
            new GenericProtocolResourceDefinition(protocol.name(), JGroupsSubsystemModel.VERSION_5_0_0, this.configurator, this.parentServiceConfiguratorFactory).register(registration);
        }

        for (EncryptProtocol protocol : EnumSet.allOf(EncryptProtocol.class)) {
            new EncryptProtocolResourceDefinition<>(protocol.name(), protocol.entryClass, this.configurator, this.parentServiceConfiguratorFactory).register(registration);
            // Add deprecated override definition for legacy variant
            new GenericProtocolResourceDefinition(protocol.name(), JGroupsSubsystemModel.VERSION_5_0_0, this.configurator, this.parentServiceConfiguratorFactory).register(registration);
        }

        for (InitialHostsProtocol protocol : EnumSet.allOf(InitialHostsProtocol.class)) {
            new SocketDiscoveryProtocolResourceDefinition<>(protocol.name(), protocol.hostTransformer, this.configurator, this.parentServiceConfiguratorFactory).register(registration);
            // Add deprecated override definition for legacy variant
            new GenericProtocolResourceDefinition(protocol.name(), JGroupsSubsystemModel.VERSION_5_0_0, this.configurator, this.parentServiceConfiguratorFactory).register(registration);
        }

        for (AuthProtocol protocol : EnumSet.allOf(AuthProtocol.class)) {
            new AuthProtocolResourceDefinition(protocol.name(), this.configurator, this.parentServiceConfiguratorFactory).register(registration);
            // Add deprecated override definition for legacy variant
            new GenericProtocolResourceDefinition(protocol.name(), JGroupsSubsystemModel.VERSION_5_0_0, this.configurator, this.parentServiceConfiguratorFactory).register(registration);
        }

        if (registration.getProcessType().isServer()) { // only auto-update legacy protocols in server processes
            for (LegacyProtocol protocol : EnumSet.allOf(LegacyProtocol.class)) {
                new LegacyProtocolResourceDefinition(protocol.name, protocol.targetName, protocol.deprecation, this.configurator, this.parentServiceConfiguratorFactory).register(registration);
            }
        }
    }
}
