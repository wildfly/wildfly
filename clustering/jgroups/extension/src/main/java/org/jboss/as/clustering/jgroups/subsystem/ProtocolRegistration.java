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

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.Registration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.function.Consumers;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;

/**
 * Registers protocol resource definitions, including any overrides.
 * @author Paul Ferraro
 */
public class ProtocolRegistration implements Registration<ManagementResourceRegistration> {

    // Enumerates protocols with custom builders or definitions
    enum ProtocolType implements Iterable<String> {
        AUTH("AUTH"),
        ENCRYPT("ASYM_ENCRYPT", "SYM_ENCRYPT"),
        JDBC("JDBC_PING"),
        MULTICAST("pbcast.NAKACK2"),
        MULTICAST_SOCKET("MPING"),
        SOCKET_DISCOVERY("TCPGOSSIP", "TCPPING"),
        ;
        private final Set<String> protocols;

        ProtocolType(String protocol) {
            this.protocols = Collections.singleton(protocol);
        }

        ProtocolType(String... protocols) {
            this.protocols = Collections.unmodifiableSet(Stream.of(protocols).collect(Collectors.toSet()));
        }

        @Override
        public Iterator<String> iterator() {
            return this.protocols.iterator();
        }

        Stream<String> stream() {
            return this.protocols.stream();
        }

        boolean contains(String protocol) {
            return this.protocols.contains(protocol);
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {

        ProtocolResourceDefinition.buildTransformation(version, parent);

        ProtocolType.MULTICAST_SOCKET.stream().map(ProtocolResourceDefinition::pathElement).forEach(path -> {
            SocketBindingProtocolResourceDefinition.addTransformations(version, parent.addChildResource(path));
        });

        ProtocolType.JDBC.stream().map(ProtocolResourceDefinition::pathElement).forEach(path -> {
            if (JGroupsModel.VERSION_5_0_0.requiresTransformation(version)) {
                parent.rejectChildResource(path);
            } else {
                JDBCProtocolResourceDefinition.addTransformations(version, parent.addChildResource(path));
            }
        });

        ProtocolType.ENCRYPT.stream().map(ProtocolResourceDefinition::pathElement).forEach(path -> {
            if (JGroupsModel.VERSION_5_0_0.requiresTransformation(version)) {
                parent.rejectChildResource(path);
            } else {
                EncryptProtocolResourceDefinition.addTransformations(version, parent.addChildResource(path));
            }
        });

        ProtocolType.SOCKET_DISCOVERY.stream().map(ProtocolResourceDefinition::pathElement).forEach(path -> {
            if (JGroupsModel.VERSION_5_0_0.requiresTransformation(version)) {
                parent.rejectChildResource(path);
            } else {
                SocketDiscoveryProtocolResourceDefinition.addTransformations(version, parent.addChildResource(path));
            }
        });

        ProtocolType.AUTH.stream().map(ProtocolResourceDefinition::pathElement).forEach(path -> {
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

    ProtocolRegistration(ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory, OperationStepHandler runtimeResourceRegistration) {
        this.parentBuilderFactory = parentBuilderFactory;
        this.descriptorConfigurator = descriptor -> descriptor.addRuntimeResourceRegistration(runtimeResourceRegistration);
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        new GenericProtocolResourceDefinition<>(this.descriptorConfigurator, address -> ProtocolType.MULTICAST.contains(address.getLastElement().getValue()) ? new MulticastProtocolConfigurationBuilder<>(address) : new ProtocolConfigurationBuilder<>(address), this.parentBuilderFactory).register(registration);

        // Override definitions for protocol types
        ProtocolType.MULTICAST_SOCKET.forEach(protocol -> new SocketBindingProtocolResourceDefinition<>(protocol, this.descriptorConfigurator, address -> new MulticastSocketProtocolConfigurationBuilder<>(address), this.parentBuilderFactory).register(registration));

        ProtocolType.JDBC.forEach(protocol -> {
            new JDBCProtocolResourceDefinition<>(protocol, this.descriptorConfigurator, this.parentBuilderFactory).register(registration);
            // Add deprecated override definition for legacy variant
            new GenericProtocolResourceDefinition<>(protocol, JGroupsModel.VERSION_5_0_0, address -> new ProtocolConfigurationBuilder<>(address), this.descriptorConfigurator, this.parentBuilderFactory).register(registration);
        });

        ProtocolType.ENCRYPT.forEach(protocol -> {
            new EncryptProtocolResourceDefinition<>(protocol, this.descriptorConfigurator, this.parentBuilderFactory).register(registration);
            // Add deprecated override definition for legacy variant
            new GenericProtocolResourceDefinition<>(protocol, JGroupsModel.VERSION_5_0_0, address -> new ProtocolConfigurationBuilder<>(address), this.descriptorConfigurator, this.parentBuilderFactory).register(registration);
        });

        ProtocolType.SOCKET_DISCOVERY.forEach(protocol -> {
            new SocketDiscoveryProtocolResourceDefinition<>(protocol, this.descriptorConfigurator, this.parentBuilderFactory).register(registration);
            // Add deprecated override definition for legacy variant
            new GenericProtocolResourceDefinition<>(protocol, JGroupsModel.VERSION_5_0_0, address -> new ProtocolConfigurationBuilder<>(address), this.descriptorConfigurator, this.parentBuilderFactory).register(registration);
        });

        ProtocolType.AUTH.forEach(protocol -> {
            new AuthProtocolResourceDefinition(protocol, this.descriptorConfigurator, this.parentBuilderFactory).register(registration);
            // Add deprecated override definition for legacy variant
            new GenericProtocolResourceDefinition<>(protocol, JGroupsModel.VERSION_5_0_0, address -> new ProtocolConfigurationBuilder<>(address), this.descriptorConfigurator, this.parentBuilderFactory).register(registration);
        });
    }
}
