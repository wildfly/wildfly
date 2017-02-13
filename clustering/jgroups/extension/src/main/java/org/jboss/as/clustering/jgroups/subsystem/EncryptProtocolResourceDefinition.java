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

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.jgroups.protocol.EncryptProtocol;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelType;
import org.jgroups.protocols.EncryptBase;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;

/**
 * Resource definition override for protocols that require an encryption key.
 * @author Paul Ferraro
 */
public class EncryptProtocolResourceDefinition<P extends EncryptBase & EncryptProtocol> extends ProtocolResourceDefinition<P> {

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        ENCRYPT_CREDENTIAL_STORE("org.wildfly.extension.undertow.application-security-domain.single-sign-on.credential-store"),
        ENCRYPT_KEY_STORE("org.wildfly.extension.undertow.application-security-domain.single-sign-on.key-store"),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name) {
            this.definition = RuntimeCapability.Builder.of(name, true).build();
        }

        @Override
        public RuntimeCapability<Void> getDefinition() {
            return this.definition;
        }

        @Override
        public RuntimeCapability<?> resolve(PathAddress address) {
            return this.definition.fromBaseCapability(address.getParent().getLastElement().getValue());
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        CREDENTIAL(CredentialReference.getAttributeBuilder(CredentialReference.CREDENTIAL_REFERENCE, CredentialReference.CREDENTIAL_REFERENCE, false).setCapabilityReference(new CapabilityReference(Capability.ENCRYPT_CREDENTIAL_STORE, CommonUnaryRequirement.CREDENTIAL_STORE)).build()),
        KEY_ALIAS("key-alias", ModelType.STRING, builder -> builder.setAllowExpression(true)),
        KEY_STORE("key-store", ModelType.STRING, builder -> builder.setCapabilityReference(new CapabilityReference(Capability.ENCRYPT_KEY_STORE, CommonUnaryRequirement.KEY_STORE))),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, UnaryOperator<SimpleAttributeDefinitionBuilder> configurator) {
            this.definition = configurator.apply(new SimpleAttributeDefinitionBuilder(name, type).setRequired(true)).build();
        }

        Attribute(AttributeDefinition definition) {
            this.definition = definition;
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void addTransformations(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {

        ProtocolResourceDefinition.addTransformations(version, builder);
    }

    public EncryptProtocolResourceDefinition(String name, Consumer<ResourceDescriptor> descriptorConfigurator, ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory) {
        super(pathElement(name), descriptorConfigurator.andThen(descriptor -> descriptor
                .addAttributes(Attribute.class)
                .addCapabilities(Capability.class)
            ), address -> new EncryptProtocolConfigurationBuilder<>(address), parentBuilderFactory);
    }
}
