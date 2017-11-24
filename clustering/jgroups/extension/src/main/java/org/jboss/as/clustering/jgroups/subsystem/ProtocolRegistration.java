/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.EnumSet;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.as.clustering.controller.Registration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.controller.RuntimeResourceRegistration;
import org.jboss.as.clustering.function.Consumers;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jgroups.PhysicalAddress;
import org.jgroups.stack.IpAddress;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;

/**
 * Registers protocol resource definitions, including any overrides.
 * @author Paul Ferraro
 */
public class ProtocolRegistration implements Registration<ManagementResourceRegistration> {

    enum AuthProtocol {
        AUTH;
    }

    enum EncryptProtocol {
        ASYM_ENCRYPT(KeyStore.PrivateKeyEntry.class),
        SYM_ENCRYPT(KeyStore.SecretKeyEntry.class),
        ;
        Class<? extends KeyStore.Entry> entryClass;

        EncryptProtocol(Class<? extends KeyStore.Entry> entryClass) {
            this.entryClass = entryClass;
        }
    }

    enum InitialHostsProtocol {
        TCPGOSSIP(InetSocketAddress.class, Function.identity()),
        TCPPING(PhysicalAddress.class, address -> new IpAddress(address.getAddress(), address.getPort())),
        ;
        Function<InetSocketAddress, ?> hostTransformer;

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

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {

        ProtocolResourceDefinition.buildTransformation(version, parent);

        EnumSet.allOf(MulticastProtocol.class).stream().map(Enum::name).map(ProtocolResourceDefinition::pathElement).forEach(path -> {
            SocketBindingProtocolResourceDefinition.addTransformations(version, parent.addChildResource(path));
        });

        EnumSet.allOf(JdbcProtocol.class).stream().map(Enum::name).map(ProtocolResourceDefinition::pathElement).forEach(path -> {
            if (JGroupsModel.VERSION_5_0_0.requiresTransformation(version)) {
                parent.rejectChildResource(path);
            } else {
                JDBCProtocolResourceDefinition.addTransformations(version, parent.addChildResource(path));
            }
        });

        EnumSet.allOf(EncryptProtocol.class).stream().map(Enum::name).map(ProtocolResourceDefinition::pathElement).forEach(path -> {
            if (JGroupsModel.VERSION_5_0_0.requiresTransformation(version)) {
                parent.rejectChildResource(path);
            } else {
                EncryptProtocolResourceDefinition.addTransformations(version, parent.addChildResource(path));
            }
        });

        EnumSet.allOf(InitialHostsProtocol.class).stream().map(Enum::name).map(ProtocolResourceDefinition::pathElement).forEach(path -> {
            if (JGroupsModel.VERSION_5_0_0.requiresTransformation(version)) {
                parent.rejectChildResource(path);
            } else {
                SocketDiscoveryProtocolResourceDefinition.addTransformations(version, parent.addChildResource(path));
            }
        });

        EnumSet.allOf(AuthProtocol.class).stream().map(Enum::name).map(ProtocolResourceDefinition::pathElement).forEach(path -> {
            if (JGroupsModel.VERSION_5_0_0.requiresTransformation(version)) {
                parent.rejectChildResource(path);
            } else {
                AuthProtocolResourceDefinition.addTransformations(version, parent.addChildResource(path));
            }
        });
    }

    private final ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory;
    private final Consumer<ResourceDescriptor> descriptorConfigurator;

    ProtocolRegistration(ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory) {
        this.parentBuilderFactory = parentBuilderFactory;
        this.descriptorConfigurator = Consumers.empty();
    }

    ProtocolRegistration(ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory, RuntimeResourceRegistration runtimeResourceRegistration) {
        this.parentBuilderFactory = parentBuilderFactory;
        this.descriptorConfigurator = descriptor -> descriptor.addRuntimeResourceRegistration(runtimeResourceRegistration);
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        new GenericProtocolResourceDefinition<>(this.descriptorConfigurator, address -> new ProtocolConfigurationBuilder<>(address), this.parentBuilderFactory).register(registration);

        // Override definitions for protocol types
        EnumSet.allOf(MulticastProtocol.class).forEach(protocol -> new SocketBindingProtocolResourceDefinition<>(protocol.name(), this.descriptorConfigurator, address -> new MulticastSocketProtocolConfigurationBuilder(address), this.parentBuilderFactory).register(registration));

        EnumSet.allOf(JdbcProtocol.class).forEach(protocol -> {
            new JDBCProtocolResourceDefinition(protocol.name(), this.descriptorConfigurator, this.parentBuilderFactory).register(registration);
            // Add deprecated override definition for legacy variant
            new GenericProtocolResourceDefinition<>(protocol.name(), JGroupsModel.VERSION_5_0_0, address -> new ProtocolConfigurationBuilder<>(address), this.descriptorConfigurator, this.parentBuilderFactory).register(registration);
        });

        EnumSet.allOf(EncryptProtocol.class).forEach(protocol -> {
            new EncryptProtocolResourceDefinition<>(protocol.name(), protocol.entryClass, this.descriptorConfigurator, this.parentBuilderFactory).register(registration);
            // Add deprecated override definition for legacy variant
            new GenericProtocolResourceDefinition<>(protocol.name(), JGroupsModel.VERSION_5_0_0, address -> new ProtocolConfigurationBuilder<>(address), this.descriptorConfigurator, this.parentBuilderFactory).register(registration);
        });

        EnumSet.allOf(InitialHostsProtocol.class).forEach(protocol -> {
            new SocketDiscoveryProtocolResourceDefinition<>(protocol.name(), protocol.hostTransformer, this.descriptorConfigurator, this.parentBuilderFactory).register(registration);
            // Add deprecated override definition for legacy variant
            new GenericProtocolResourceDefinition<>(protocol.name(), JGroupsModel.VERSION_5_0_0, address -> new ProtocolConfigurationBuilder<>(address), this.descriptorConfigurator, this.parentBuilderFactory).register(registration);
        });

        EnumSet.allOf(AuthProtocol.class).forEach(protocol -> {
            new AuthProtocolResourceDefinition(protocol.name(), this.descriptorConfigurator, this.parentBuilderFactory).register(registration);
            // Add deprecated override definition for legacy variant
            new GenericProtocolResourceDefinition<>(protocol.name(), JGroupsModel.VERSION_5_0_0, address -> new ProtocolConfigurationBuilder<>(address), this.descriptorConfigurator, this.parentBuilderFactory).register(registration);
        });
    }
}
