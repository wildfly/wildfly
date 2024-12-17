/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import javax.crypto.Cipher;

import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.jgroups.auth.CipherAuthToken;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.controller.security.CredentialReferenceWriteAttributeHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * @author Paul Ferraro
 */
public class CipherAuthTokenResourceDefinition extends AuthTokenResourceDefinition<CipherAuthToken> {

    static final PathElement PATH = pathElement("cipher");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        KEY_CREDENTIAL(CredentialReference.getAttributeBuilder("key-credential-reference", null, false, CapabilityReferenceRecorder.builder(CAPABILITY, CommonServiceDescriptor.CREDENTIAL_STORE).build()).build()),
        KEY_ALIAS("key-alias", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(true);
            }
        },
        KEY_STORE("key-store", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setCapabilityReference(CapabilityReferenceRecorder.builder(CAPABILITY, CommonServiceDescriptor.KEY_STORE).build());
            }
        },
        ALGORITHM("algorithm", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(true)
                        .setRequired(false)
                        .setDefaultValue(new ModelNode("RSA"))
                        ;
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setRequired(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        Attribute(AttributeDefinition definition) {
            this.definition = definition;
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
            return builder;
        }
    }

    static class ResourceDescriptorTransformer implements UnaryOperator<ResourceDescriptor> {
        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return descriptor.addAttributes(EnumSet.complementOf(EnumSet.of(Attribute.KEY_CREDENTIAL)))
                    .addAttribute(Attribute.KEY_CREDENTIAL, new CredentialReferenceWriteAttributeHandler(Attribute.KEY_CREDENTIAL.getDefinition()));
        }
    }

    CipherAuthTokenResourceDefinition() {
        super(PATH, new ResourceDescriptorTransformer());
    }

    @Override
    public Map.Entry<Function<String, CipherAuthToken>, Consumer<RequirementServiceBuilder<?>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        String keyStoreName = Attribute.KEY_STORE.resolveModelAttribute(context, model).asString();
        String keyAlias = Attribute.KEY_ALIAS.resolveModelAttribute(context, model).asString();
        String transformation = Attribute.ALGORITHM.resolveModelAttribute(context, model).asString();

        ServiceDependency<KeyStore> keyStore = ServiceDependency.on(CommonServiceDescriptor.KEY_STORE, keyStoreName);
        ServiceDependency<CredentialSource> keyCredentialSource = ServiceDependency.from(CredentialReference.getCredentialSourceDependency(context, Attribute.KEY_CREDENTIAL.getDefinition(), model));

        return Map.entry(new Function<>() {
            @Override
            public CipherAuthToken apply(String authValue) {
                KeyStore store = keyStore.get();
                try {
                    if (!store.containsAlias(keyAlias)) {
                        throw JGroupsLogger.ROOT_LOGGER.keyEntryNotFound(keyAlias);
                    }
                    if (!store.entryInstanceOf(keyAlias, KeyStore.PrivateKeyEntry.class)) {
                        throw JGroupsLogger.ROOT_LOGGER.unexpectedKeyStoreEntryType(keyAlias, KeyStore.PrivateKeyEntry.class.getSimpleName());
                    }
                    PasswordCredential credential = keyCredentialSource.get().getCredential(PasswordCredential.class);
                    if (credential == null) {
                        throw JGroupsLogger.ROOT_LOGGER.unexpectedCredentialSource();
                    }
                    ClearPassword password = credential.getPassword(ClearPassword.class);
                    if (password == null) {
                        throw JGroupsLogger.ROOT_LOGGER.unexpectedCredentialSource();
                    }
                    KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) store.getEntry(keyAlias, new KeyStore.PasswordProtection(password.getPassword()));
                    KeyPair pair = new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey());
                    Cipher cipher = Cipher.getInstance(transformation);
                    return new CipherAuthToken(cipher, pair, authValue.getBytes(StandardCharsets.UTF_8));
                } catch (GeneralSecurityException | IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }, keyStore.andThen(keyCredentialSource));
    }
}
