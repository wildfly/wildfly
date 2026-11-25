/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.crypto.Cipher;

import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.clustering.controller.CredentialReferenceAttributeDefinition;
import org.jboss.as.clustering.jgroups.auth.CipherAuthToken;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.security.CredentialReferenceWriteAttributeHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a cipher authentication token for use by the AUTH protocol.
 * @author Paul Ferraro
 */
public class CipherAuthTokenResourceDefinitionRegistrar extends AuthTokenResourceDefinitionRegistrar<CipherAuthToken> {

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

    CipherAuthTokenResourceDefinitionRegistrar() {
        super(Token.CIPHER);
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return builder.addAttribute(KEY_CREDENTIAL, CredentialReferenceWriteAttributeHandler.INSTANCE)
                .addAttributes(List.of(KEY_STORE))
                .provideAttributes(EnumSet.allOf(Attribute.class))
                ;
    }

    @Override
    public ServiceDependency<Function<byte[], CipherAuthToken>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        String keyAlias = Attribute.KEY_ALIAS.resolveModelAttribute(context, model).asString();
        String transformation = Attribute.ALGORITHM.resolveModelAttribute(context, model).asString();

        return KEY_STORE.resolve(context, model).combine(KEY_CREDENTIAL.resolve(context, model).map(CLEAR_PASSWORD_CREDENTIAL), new BiFunction<>() {
            @Override
            public Function<byte[], CipherAuthToken> apply(KeyStore store, char[] password) {
                return new Function<>() {
                    @Override
                    public CipherAuthToken apply(byte[] secret) {
                        try {
                            if (!store.containsAlias(keyAlias)) {
                                throw JGroupsLogger.ROOT_LOGGER.keyEntryNotFound(keyAlias);
                            }
                            if (!store.entryInstanceOf(keyAlias, KeyStore.PrivateKeyEntry.class)) {
                                throw JGroupsLogger.ROOT_LOGGER.unexpectedKeyStoreEntryType(keyAlias, KeyStore.PrivateKeyEntry.class.getSimpleName());
                            }
                            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) store.getEntry(keyAlias, new KeyStore.PasswordProtection(password));
                            KeyPair pair = new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey());
                            Cipher cipher = Cipher.getInstance(transformation);
                            return new CipherAuthToken(cipher, pair, secret);
                        } catch (GeneralSecurityException e) {
                            throw new IllegalArgumentException(e);
                        }
                    }
                };
            }
        });
    }
}
