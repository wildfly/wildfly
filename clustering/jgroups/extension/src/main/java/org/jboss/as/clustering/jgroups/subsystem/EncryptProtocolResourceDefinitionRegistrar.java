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
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.MapOperations;
import org.jboss.as.controller.security.CredentialReferenceWriteAttributeHandler;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.Encrypt;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for an encryption protocol.
 * @author Paul Ferraro
 */
public class EncryptProtocolResourceDefinitionRegistrar<E extends KeyStore.Entry, P extends Encrypt<E>> extends AbstractProtocolResourceDefinitionRegistrar<P> {

    public EncryptProtocolResourceDefinitionRegistrar(EncryptProtocolResourceDescription description, Class<E> entryClass, ResourceOperationRuntimeHandler parentRuntimeHandler) {
        super(new ProtocolResourceDescriptorConfigurator<>() {
            @Override
            public ProtocolResourceDescription getResourceDescription() {
                return description;
            }

            @Override
            public ResourceOperationRuntimeHandler getParentRuntimeHandler() {
                return parentRuntimeHandler;
            }

            @Override
            public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
                return builder.addCapability(ProtocolResourceDescription.CAPABILITY)
                        .addAttributes(description.getAttributes().filter(Predicate.not(EncryptProtocolResourceDescription.KEY_CREDENTIAL::equals)).collect(Collectors.toList()))
                        .addAttribute(EncryptProtocolResourceDescription.KEY_CREDENTIAL, CredentialReferenceWriteAttributeHandler.INSTANCE)
                        .withOperationTransformation(ModelDescriptionConstants.ADD, new LegacyAddOperationTransformation(List.of(EncryptProtocolResourceDescription.KEY_ALIAS, EncryptProtocolResourceDescription.KEY_CREDENTIAL, EncryptProtocolResourceDescription.KEY_STORE)))
                        .withOperationTransformation(Set.of(ModelDescriptionConstants.REMOVE, MapOperations.MAP_GET_DEFINITION.getName(), MapOperations.MAP_PUT_DEFINITION.getName(), MapOperations.MAP_REMOVE_DEFINITION.getName(), MapOperations.MAP_CLEAR_DEFINITION.getName()), LEGACY_OPERATION_TRANSFORMER)
                        ;
            }

            @Override
            public ServiceDependency<ProtocolConfiguration<P>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                String keyAlias = EncryptProtocolResourceDescription.KEY_ALIAS.resolveModelAttribute(context, model).asString();

                ServiceDependency<ProtocolConfiguration<P>> protocol = ProtocolResourceDescriptorConfigurator.super.resolve(context, model);
                ServiceDependency<KeyStore> keyStore = EncryptProtocolResourceDescription.KEY_STORE.resolve(context, model);
                ServiceDependency<CredentialSource> credentialSource = EncryptProtocolResourceDescription.KEY_CREDENTIAL.resolve(context, model);
                return new ServiceDependency<>() {
                    @Override
                    public void accept(RequirementServiceBuilder<?> builder) {
                        protocol.accept(builder);
                        keyStore.accept(builder);
                        credentialSource.accept(builder);
                    }

                    @Override
                    public ProtocolConfiguration<P> get() {
                        return new ProtocolConfigurationDecorator<>(protocol.get()) {
                            @Override
                            public P createProtocol(ChannelFactoryConfiguration stackConfiguration) {
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
                };
            }
        });
    }
}
