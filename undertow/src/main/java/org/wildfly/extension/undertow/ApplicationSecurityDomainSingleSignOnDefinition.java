/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import javax.net.ssl.SSLContext;

import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.controller.security.CredentialReferenceWriteAttributeHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.web.container.SingleSignOnManagerConfiguration;
import org.wildfly.clustering.web.container.SingleSignOnManagerServiceInstallerProvider;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.extension.undertow.sso.elytron.NonDistributableSingleSignOnManagementProvider;
import org.wildfly.extension.undertow.sso.elytron.SingleSignOnIdentifierFactory;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.http.util.sso.DefaultSingleSignOnSessionFactory;
import org.wildfly.security.http.util.sso.SingleSignOnConfiguration;
import org.wildfly.security.http.util.sso.SingleSignOnManager;
import org.wildfly.security.http.util.sso.SingleSignOnSessionFactory;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class ApplicationSecurityDomainSingleSignOnDefinition extends SingleSignOnDefinition {

    static final UnaryServiceDescriptor<SingleSignOnConfiguration> SSO_CONFIGURATION = UnaryServiceDescriptor.of("org.wildfly.undertow.application-security-domain.sso.configuration", SingleSignOnConfiguration.class);
    static final UnaryServiceDescriptor<SingleSignOnSessionFactory> SSO_SESSION_FACTORY = UnaryServiceDescriptor.of("org.wildfly.undertow.application-security-domain.sso.factory", SingleSignOnSessionFactory.class);
    private static final UnaryServiceDescriptor<SingleSignOnManager> SSO_MANAGER = UnaryServiceDescriptor.of("org.wildfly.undertow.application-security-domain.sso.manager", SingleSignOnManager.class);

    private static final RuntimeCapability<Void> CONFIGURATION_CAPABILITY = RuntimeCapability.Builder.of(SSO_CONFIGURATION).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();
    private static final RuntimeCapability<Void> SESSION_FACTORY_CAPABILITY = RuntimeCapability.Builder.of(SSO_SESSION_FACTORY).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();

    enum Attribute implements AttributeDefinitionProvider {
        CREDENTIAL(CredentialReference.getAttributeBuilder(CredentialReference.CREDENTIAL_REFERENCE, CredentialReference.CREDENTIAL_REFERENCE, false, CapabilityReferenceRecorder.builder(CONFIGURATION_CAPABILITY, CommonServiceDescriptor.CREDENTIAL_STORE).build()).setAccessConstraints(SensitiveTargetAccessConstraintDefinition.CREDENTIAL).build()),
        KEY_ALIAS("key-alias", ModelType.STRING, builder -> builder.setAllowExpression(true).addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SSL_REF)),
        KEY_STORE("key-store", ModelType.STRING, builder -> builder.setCapabilityReference(CapabilityReferenceRecorder.builder(CONFIGURATION_CAPABILITY, CommonServiceDescriptor.KEY_STORE).build()).addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SSL_REF)),
        SSL_CONTEXT("client-ssl-context", ModelType.STRING, builder -> builder.setRequired(false).setCapabilityReference(CapabilityReferenceRecorder.builder(CONFIGURATION_CAPABILITY, CommonServiceDescriptor.SSL_CONTEXT).build()).setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SSL_REF)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, UnaryOperator<SimpleAttributeDefinitionBuilder> configurator) {
            this.definition = configurator.apply(new SimpleAttributeDefinitionBuilder(name, type).setRequired(true)).build();
        }

        Attribute(AttributeDefinition definition) {
            this.definition = definition;
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    private final SingleSignOnManagerServiceInstallerProvider provider = ServiceLoader.load(SingleSignOnManagerServiceInstallerProvider.class, SingleSignOnManagerServiceInstallerProvider.class.getClassLoader()).findFirst().orElse(NonDistributableSingleSignOnManagementProvider.INSTANCE);

    ApplicationSecurityDomainSingleSignOnDefinition(ResourceOperationRuntimeHandler parentHandler) {
        super(ResourceDefinition::builder, new BiFunction<>() {
            @Override
            public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder, ResourceServiceConfigurator configurator) {
                return builder.provideAttributes(EnumSet.complementOf(EnumSet.of(Attribute.CREDENTIAL)))
                        .addAttribute(Attribute.CREDENTIAL.get(), new CredentialReferenceWriteAttributeHandler(Attribute.CREDENTIAL.get()))
                        .addCapabilities(List.of(CONFIGURATION_CAPABILITY, SESSION_FACTORY_CAPABILITY))
                        .withRuntimeHandler(ResourceOperationRuntimeHandler.combine(ResourceOperationRuntimeHandler.configureService(configurator), ResourceOperationRuntimeHandler.restartParent(parentHandler)))
                        ;
            }
        });
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {

        ResourceServiceInstaller configurationInstaller = CapabilityServiceInstaller.builder(CONFIGURATION_CAPABILITY, this.resolve(context, model)).build();

        String securityDomainName = context.getCurrentAddress().getParent().getLastElement().getValue();

        Supplier<String> generator = new SingleSignOnIdentifierFactory();
        SingleSignOnManagerConfiguration configuration = new SingleSignOnManagerConfiguration() {
            @Override
            public String getSecurityDomainName() {
                return securityDomainName;
            }

            @Override
            public Supplier<String> getIdentifierGenerator() {
                return generator;
            }
        };
        ResourceServiceInstaller managerInstaller = this.provider.getServiceInstaller(configuration);

        ServiceDependency<SingleSignOnManager> manager = ServiceDependency.on(SSO_MANAGER, securityDomainName);
        ServiceDependency<KeyStore> keyStore = ServiceDependency.on(CommonServiceDescriptor.KEY_STORE, Attribute.KEY_STORE.resolveModelAttribute(context, model).asString());
        String keyAlias = Attribute.KEY_ALIAS.resolveModelAttribute(context, model).asString();
        ServiceDependency<CredentialSource> credentialSource = ServiceDependency.from(CredentialReference.getCredentialSourceDependency(context, Attribute.CREDENTIAL.get(), model));
        String sslContextName = Attribute.SSL_CONTEXT.resolveModelAttribute(context, model).asStringOrNull();
        ServiceDependency<SSLContext> sslContext = (sslContextName != null) ? ServiceDependency.on(CommonServiceDescriptor.SSL_CONTEXT, sslContextName) : ServiceDependency.of(null);

        Supplier<SingleSignOnSessionFactory> factory = new Supplier<>() {
            @Override
            public SingleSignOnSessionFactory get() {
                KeyStore store = keyStore.get();
                CredentialSource source = credentialSource.get();
                try {
                    if (!store.containsAlias(keyAlias)) {
                        throw UndertowLogger.ROOT_LOGGER.missingKeyStoreEntry(keyAlias);
                    }
                    if (!store.entryInstanceOf(keyAlias, KeyStore.PrivateKeyEntry.class)) {
                        throw UndertowLogger.ROOT_LOGGER.keyStoreEntryNotPrivate(keyAlias);
                    }
                    PasswordCredential credential = source.getCredential(PasswordCredential.class);
                    if (credential == null) {
                        throw UndertowLogger.ROOT_LOGGER.missingCredential(source.toString());
                    }
                    ClearPassword password = credential.getPassword(ClearPassword.class);
                    if (password == null) {
                        throw UndertowLogger.ROOT_LOGGER.credentialNotClearPassword(credential.toString());
                    }
                    KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) store.getEntry(keyAlias, new KeyStore.PasswordProtection(password.getPassword()));
                    KeyPair keyPair = new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey());
                    Optional<SSLContext> context = Optional.ofNullable(sslContext.get());
                    return new DefaultSingleSignOnSessionFactory(manager.get(), keyPair, connection -> context.ifPresent(ctx -> connection.setSSLSocketFactory(ctx.getSocketFactory())));
                } catch (GeneralSecurityException | IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        };
        ResourceServiceInstaller factoryInstaller = CapabilityServiceInstaller.builder(SESSION_FACTORY_CAPABILITY, factory).blocking()
                .requires(List.of(manager, keyStore, credentialSource, sslContext))
                .build();

        return ResourceServiceInstaller.combine(configurationInstaller, managerInstaller, factoryInstaller);
    }
}
