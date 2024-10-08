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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.clustering.controller.CredentialReferenceAttributeDefinition;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.MapOperations;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.security.CredentialReferenceWriteAttributeHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jgroups.protocols.Encrypt;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for an encryption protocol.
 * @author Paul Ferraro
 */
public class EncryptProtocolResourceDefinitionRegistrar<E extends KeyStore.Entry, P extends Encrypt<E>> extends AbstractProtocolResourceDefinitionRegistrar<P> {

    static final CredentialReferenceAttributeDefinition KEY_CREDENTIAL = new CredentialReferenceAttributeDefinition.Builder("key-credential-reference", CAPABILITY).build();
    static final CapabilityReferenceAttributeDefinition<KeyStore> KEY_STORE = new CapabilityReferenceAttributeDefinition.Builder<>("key-store", CapabilityReference.builder(CAPABILITY, CommonServiceDescriptor.KEY_STORE).build()).build();
    static final AttributeDefinition KEY_ALIAS = new SimpleAttributeDefinitionBuilder("key-alias", ModelType.STRING)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    private static final Collection<AttributeDefinition> ATTRIBUTES = List.of(KEY_CREDENTIAL, KEY_ALIAS, KEY_STORE);

    static Stream<AttributeDefinition> attributes() {
        return Stream.concat(ATTRIBUTES.stream(), AbstractProtocolResourceDefinitionRegistrar.attributes());
    }

    public EncryptProtocolResourceDefinitionRegistrar(String name, Class<E> entryClass, ResourceOperationRuntimeHandler parentRuntimeHandler) {
        super(new ProtocolResourceRegistration<>() {
            @Override
            public PathElement getPathElement() {
                return pathElement(name);
            }

            @Override
            public ResourceOperationRuntimeHandler getParentRuntimeHandler() {
                return parentRuntimeHandler;
            }

            @Override
            public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
                return ProtocolResourceRegistration.super.apply(builder)
                        .addAttributes(List.of(KEY_STORE, KEY_ALIAS))
                        .addAttribute(KEY_CREDENTIAL, CredentialReferenceWriteAttributeHandler.INSTANCE)
                        .withOperationTransformation(ModelDescriptionConstants.ADD, new LegacyAddOperationTransformation(ATTRIBUTES))
                        .withOperationTransformation(Set.of(ModelDescriptionConstants.REMOVE, MapOperations.MAP_GET_DEFINITION.getName(), MapOperations.MAP_PUT_DEFINITION.getName(), MapOperations.MAP_REMOVE_DEFINITION.getName(), MapOperations.MAP_CLEAR_DEFINITION.getName()), LEGACY_OPERATION_TRANSFORMER)
                        ;
            }

            @Override
            public ServiceDependency<ProtocolConfiguration<P>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                String keyAlias = KEY_ALIAS.resolveModelAttribute(context, model).asString();

                ServiceDependency<ProtocolConfiguration<P>> protocol = ProtocolResourceRegistration.super.resolve(context, model);
                ServiceDependency<KeyStore> keyStore = KEY_STORE.resolve(context, model);
                ServiceDependency<CredentialSource> credentialSource = KEY_CREDENTIAL.resolve(context, model);
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
