/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.security.KeyStore;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.clustering.controller.CredentialReferenceAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;

/**
 * Descriptions of encryption protocol resources.
 * @author Paul Ferraro
 */
public enum EncryptProtocolResourceDescription implements ProtocolResourceDescription {
    ASYM_ENCRYPT(KeyStore.PrivateKeyEntry.class),
    SYM_ENCRYPT(KeyStore.SecretKeyEntry.class),
    ;

    static final CredentialReferenceAttributeDefinition KEY_CREDENTIAL = new CredentialReferenceAttributeDefinition.Builder("key-credential-reference", CAPABILITY).build();
    static final CapabilityReferenceAttributeDefinition<KeyStore> KEY_STORE = new CapabilityReferenceAttributeDefinition.Builder<>("key-store", CapabilityReference.builder(CAPABILITY, CommonServiceDescriptor.KEY_STORE).build()).build();
    static final AttributeDefinition KEY_ALIAS = new SimpleAttributeDefinitionBuilder("key-alias", ModelType.STRING)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    private final PathElement path = ProtocolResourceDescription.pathElement(this.name());
    private final Class<? extends KeyStore.Entry> entryClass;

    EncryptProtocolResourceDescription(Class<? extends KeyStore.Entry> entryClass) {
        this.entryClass = entryClass;
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(Stream.of(KEY_CREDENTIAL, KEY_STORE, KEY_ALIAS), ProtocolResourceDescription.super.getAttributes());
    }

    Class<? extends KeyStore.Entry> getKeyStoreEntryClass() {
        return this.entryClass;
    }
}
