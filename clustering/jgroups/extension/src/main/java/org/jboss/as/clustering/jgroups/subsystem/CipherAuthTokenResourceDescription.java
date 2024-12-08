/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.security.KeyStore;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.clustering.controller.CredentialReferenceAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.security.CredentialReferenceWriteAttributeHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;

/**
 * @author Paul Ferraro
 */
public enum CipherAuthTokenResourceDescription implements AuthTokenResourceDescription {
    INSTANCE;

    static final CredentialReferenceAttributeDefinition KEY_CREDENTIAL = new CredentialReferenceAttributeDefinition.Builder("key-credential-reference", CAPABILITY).build();
    static final CapabilityReferenceAttributeDefinition<KeyStore> KEY_STORE = new CapabilityReferenceAttributeDefinition.Builder<>("key-store", CapabilityReference.builder(CAPABILITY, CommonServiceDescriptor.KEY_STORE).build()).build();

    enum Attribute implements AttributeDefinitionProvider {
        KEY_ALIAS("key-alias", ModelType.STRING, null),
        ALGORITHM("algorithm", ModelType.STRING, new ModelNode("RSA")),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setRequired(defaultValue == null)
                    .setAllowExpression(true)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    private final PathElement path = AuthTokenResourceDescription.pathElement("cipher");

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(AuthTokenResourceDescription.super.getAttributes(), Stream.concat(Stream.of(KEY_CREDENTIAL, KEY_STORE), ResourceDescriptor.stream(EnumSet.allOf(Attribute.class))));
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return AuthTokenResourceDescription.super.apply(builder)
                .provideAttributes(EnumSet.allOf(Attribute.class))
                .addAttribute(KEY_CREDENTIAL, CredentialReferenceWriteAttributeHandler.INSTANCE)
                .addAttributes(List.of(KEY_STORE))
                ;
    }
}
