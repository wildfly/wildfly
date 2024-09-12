/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.controller.security.CredentialReferenceWriteAttributeHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jgroups.protocols.Encrypt;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Resource definition override for protocols that require an encryption key.
 * @author Paul Ferraro
 */
public class EncryptProtocolResourceDefinition<E extends KeyStore.Entry, P extends Encrypt<E>> extends ProtocolResourceDefinition<P> {

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

    private static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        private final UnaryOperator<ResourceDescriptor> configurator;

        ResourceDescriptorConfigurator(UnaryOperator<ResourceDescriptor> configurator) {
            this.configurator = configurator;
        }

        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return this.configurator.apply(descriptor)
                    .addAttributes(EnumSet.complementOf(EnumSet.of(Attribute.KEY_CREDENTIAL)))
                    .addAttribute(Attribute.KEY_CREDENTIAL, new CredentialReferenceWriteAttributeHandler(Attribute.KEY_CREDENTIAL.getDefinition()))
                    .setAddOperationTransformation(new LegacyAddOperationTransformation(Attribute.class))
                    .setOperationTransformation(LEGACY_OPERATION_TRANSFORMER)
                    ;
        }
    }

    private final Class<E> entryClass;

    public EncryptProtocolResourceDefinition(String name, Class<E> entryClass, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfigurator parentServiceConfigurator) {
        super(pathElement(name), new ResourceDescriptorConfigurator(configurator), parentServiceConfigurator);
        this.entryClass = entryClass;
    }

    @Override
    public Map.Entry<Function<ProtocolConfiguration<P>, ProtocolConfiguration<P>>, Consumer<RequirementServiceBuilder<?>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        String keyStoreName = Attribute.KEY_STORE.resolveModelAttribute(context, model).asString();
        String keyAlias = Attribute.KEY_ALIAS.resolveModelAttribute(context, model).asString();
        Class<E> entryClass = this.entryClass;

        ServiceDependency<KeyStore> keyStore = ServiceDependency.on(CommonServiceDescriptor.KEY_STORE, keyStoreName);
        ServiceDependency<CredentialSource> credentialSource = ServiceDependency.from(CredentialReference.getCredentialSourceDependency(context, Attribute.KEY_CREDENTIAL.getDefinition(), model));

        return Map.entry(new UnaryOperator<>() {
            @Override
            public ProtocolConfiguration<P> apply(ProtocolConfiguration<P> configuration) {
                return new ProtocolConfigurationDecorator<>(configuration) {
                    @Override
                    public P createProtocol(ProtocolStackConfiguration stackConfiguration) {
                        P protocol = super.createProtocol(stackConfiguration);
                        KeyStore store = keyStore.get();
                        try {
                            if (!store.containsAlias(keyAlias)) {
                                throw JGroupsLogger.ROOT_LOGGER.keyEntryNotFound(keyAlias);
                            }
                            PasswordCredential credential = credentialSource.get().getCredential(PasswordCredential.class);
                            if (credential == null) {
                                throw JGroupsLogger.ROOT_LOGGER.unexpectedCredentialSource();
                            }
                            ClearPassword password = credential.getPassword(ClearPassword.class);
                            if (password == null) {
                                throw JGroupsLogger.ROOT_LOGGER.unexpectedCredentialSource();
                            }
                            if (!store.entryInstanceOf(keyAlias, entryClass)) {
                                throw JGroupsLogger.ROOT_LOGGER.unexpectedKeyStoreEntryType(keyAlias, entryClass.getSimpleName());
                            }
                            KeyStore.Entry entry = store.getEntry(keyAlias, new KeyStore.PasswordProtection(password.getPassword()));
                            protocol.setKeyStoreEntry(entryClass.cast(entry));
                        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | UnrecoverableEntryException e) {
                            throw new IllegalArgumentException(e);
                        }
                        return protocol;
                    }
                };
            }
        }, keyStore.andThen(credentialSource));
    }
}
