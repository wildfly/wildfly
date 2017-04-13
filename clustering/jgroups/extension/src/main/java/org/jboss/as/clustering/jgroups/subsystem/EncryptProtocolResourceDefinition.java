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

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import javax.security.auth.x500.X500Principal;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.clustering.jgroups.protocol.EncryptProtocol;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jgroups.protocols.ASYM_ENCRYPT;
import org.jgroups.protocols.EncryptBase;
import org.jgroups.protocols.SYM_ENCRYPT;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.common.function.ExceptionBiFunction;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.security.x500.cert.X509CertificateBuilder;

/**
 * Resource definition override for protocols that require an encryption key.
 * @author Paul Ferraro
 */
public class EncryptProtocolResourceDefinition<P extends EncryptBase & EncryptProtocol> extends ProtocolResourceDefinition<P> {

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        CREDENTIAL(CredentialReference.getAttributeBuilder(CredentialReference.CREDENTIAL_REFERENCE, CredentialReference.CREDENTIAL_REFERENCE, false, new CapabilityReference(Capability.PROTOCOL, CommonUnaryRequirement.CREDENTIAL_STORE)).build()),
        KEY_ALIAS("key-alias", ModelType.STRING, builder -> builder.setAllowExpression(true)),
        KEY_STORE("key-store", ModelType.STRING, builder -> builder.setCapabilityReference(new CapabilityReference(Capability.PROTOCOL, CommonUnaryRequirement.KEY_STORE))),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, UnaryOperator<SimpleAttributeDefinitionBuilder> configurator) {
            this.definition = configurator.apply(new SimpleAttributeDefinitionBuilder(name, type)
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
    }

    static void addTransformations(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {

        ProtocolResourceDefinition.addTransformations(version, builder);
    }

    // Translator for legacy CLI operations (with missing elytron attributes)
    private static final OperationStepHandler ADD_OPERATION_TRANSLATOR = new OperationStepHandler() {
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            if (!operation.hasDefined(Attribute.KEY_STORE.getName()) && !operation.hasDefined(Attribute.KEY_ALIAS.getName()) && !operation.hasDefined(Attribute.CREDENTIAL.getName())) {
                // Locate subsystem address
                PathAddress subsystemAddress = context.getCurrentAddress().getParent();
                while (!subsystemAddress.getLastElement().getKey().equals(ModelDescriptionConstants.SUBSYSTEM)) {
                    subsystemAddress = subsystemAddress.getParent();
                }
                Map<String, ModelNode> properties = ModelNodes.optionalPropertyList(AbstractProtocolResourceDefinition.Attribute.PROPERTIES.resolveModelAttribute(context, operation)).orElse(Collections.emptyList()).stream().collect(Collectors.toMap(Property::getName, Property::getValue));
                String keyStoreName = "jgroups-" + context.getCurrentAddress().getParent().getLastElement().getValue();
                LegacyEncryptDescriptor descriptor = LegacyEncryptDescriptorFactory.valueOf(context.getCurrentAddressValue()).apply(keyStoreName, properties);

                // Synthesize a key-store resource that corresponds to the legacy properties
                PathAddress keyStoreAddress = subsystemAddress.getParent().append(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "elytron"), PathElement.pathElement("key-store", keyStoreName));
                ModelNode keyStoreOperation = Util.createAddOperation(keyStoreAddress);
                keyStoreOperation.get(ModelDescriptionConstants.TYPE).set(descriptor.getKeyStoreType());
                keyStoreOperation.get(ModelDescriptionConstants.PATH).set(descriptor.getKeyStorePath());
                keyStoreOperation.get(ModelDescriptionConstants.REQUIRED).set(true);
                keyStoreOperation.get(CredentialReference.CREDENTIAL_REFERENCE).get(CredentialReference.CLEAR_TEXT).set(descriptor.getKeyStorePassword());

                OperationStepHandler addHandler = context.getRootResourceRegistration().getOperationHandler(keyStoreAddress, ModelDescriptionConstants.ADD);
                if (addHandler == null) {
                    throw JGroupsLogger.ROOT_LOGGER.operationNotDefined(ModelDescriptionConstants.ADD, keyStoreAddress.toCLIStyleString());
                }
                context.addStep(keyStoreOperation, addHandler, OperationContext.Stage.MODEL);

                // Populate elytron attributes based on synthesized resource
                operation.get(Attribute.KEY_STORE.getName()).set(keyStoreName);
                operation.get(Attribute.KEY_ALIAS.getName()).set(descriptor.getKeyAlias());
                operation.get(Attribute.CREDENTIAL.getName()).get(CredentialReference.CLEAR_TEXT).set(descriptor.getKeyPassword());
            }
        }
    };

    private interface LegacyEncryptDescriptor {
        ModelNode getKeyStoreType();
        ModelNode getKeyStorePath();
        ModelNode getKeyStorePassword();
        ModelNode getKeyAlias();
        ModelNode getKeyPassword();
    }

    private enum LegacyEncryptDescriptorFactory implements ExceptionBiFunction<String, Map<String, ModelNode>, LegacyEncryptDescriptor, OperationFailedException> {
        ASYM_ENCRYPT((keyStoreName, properties) -> new LegacyAsymmetricEncryptDescriptor(keyStoreName, properties)),
        SYM_ENCRYPT((keyStoreName, properties) -> new LegacySymmetricEncryptDescriptor(properties)),
        ;
        private final ExceptionBiFunction<String, Map<String, ModelNode>, LegacyEncryptDescriptor, OperationFailedException> factory;

        LegacyEncryptDescriptorFactory(ExceptionBiFunction<String, Map<String, ModelNode>, LegacyEncryptDescriptor, OperationFailedException> factory) {
            this.factory = factory;
        }

        @Override
        public LegacyEncryptDescriptor apply(String keyStoreName, Map<String, ModelNode> properties) throws OperationFailedException {
            return this.factory.apply(keyStoreName, properties);
        }
    }

    private static class LegacySymmetricEncryptDescriptor implements LegacyEncryptDescriptor {
        private static final SYM_ENCRYPT DEFAULTS = new SYM_ENCRYPT();
        private final Map<String, ModelNode> properties;

        LegacySymmetricEncryptDescriptor(Map<String, ModelNode> properties) {
            this.properties = properties;
        }

        @Override
        public ModelNode getKeyStoreType() {
            return this.properties.getOrDefault("keystore_type", new ModelNode("JCEKS"));
        }

        @Override
        public ModelNode getKeyStorePath() {
            return this.properties.get("keystore_name");
        }

        @Override
        public ModelNode getKeyStorePassword() {
            return this.properties.getOrDefault("store_password", new ModelNode(DEFAULTS.storePassword()));
        }

        @Override
        public ModelNode getKeyAlias() {
            return this.properties.getOrDefault("alias", new ModelNode(DEFAULTS.alias()));
        }

        @Override
        public ModelNode getKeyPassword() {
            return this.properties.getOrDefault("key_password", this.getKeyStorePassword());
        }
    }

    private static class LegacyAsymmetricEncryptDescriptor implements LegacyEncryptDescriptor {
        private static final ASYM_ENCRYPT DEFAULTS = new ASYM_ENCRYPT();
        private static final char[] PASSWORD_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
        private final SecureRandom random = new SecureRandom();
        private final char[] keyStorePassword = this.generatePassword(32);
        private final char[] keyPassword = this.generatePassword(32);
        private final String keyStoreType = KeyStore.getDefaultType();
        private final String alias;
        private final String path;

        LegacyAsymmetricEncryptDescriptor(String keyStoreName, Map<String, ModelNode> properties) throws OperationFailedException {
            String provider = Optional.ofNullable(properties.get("provider")).map(ModelNode::asString).orElse(null);
            String transformation = Optional.ofNullable(properties.get("asym_algorithm")).map(ModelNode::asString).orElse(DEFAULTS.asymAlgorithm());
            int index = transformation.indexOf('/');
            String algorithm = (index < 0) ? transformation : transformation.substring(0, index);
            int keyLength = Optional.ofNullable(properties.get("asym_keylength")).map(ModelNode::asInt).orElse(DEFAULTS.asymKeylength());
            this.alias = DEFAULTS.getClass().getSimpleName();
            this.path = keyStoreName + ".keystore";
            try {
                KeyPairGenerator generator = (provider != null) ? KeyPairGenerator.getInstance(algorithm, provider) : KeyPairGenerator.getInstance(algorithm);
                generator.initialize(keyLength, this.random);
                KeyPair pair = generator.generateKeyPair();

                PrivilegedAction<String> action = () -> System.getProperty("user.name");
                // Details of the certificate are not critical - as we ultimately only care about extracting the public key.
                X500Principal issuer = new X500Principal("UID=" + WildFlySecurityManager.doUnchecked(action));
                // Create self-signed certificate using generated key pair
                X509Certificate certificate = new X509CertificateBuilder()
                        .setPublicKey(pair.getPublic())
                        .setSignatureAlgorithmName("SHA1with" + algorithm)
                        .setSigningKey(pair.getPrivate())
                        .setIssuerDn(issuer)
                        .setSubjectDn(issuer)
                        .build();

                KeyStore store = KeyStore.getInstance(this.keyStoreType);
                store.load(null, this.keyStorePassword);
                // Add key entry containing private key and certificate referencing public key
                store.setKeyEntry(this.alias, pair.getPrivate(), this.keyPassword, new Certificate[] { certificate });
                // Persist key store to the expected path
                store.store(new FileOutputStream(this.path), this.keyStorePassword);
            } catch (GeneralSecurityException | IOException e) {
                throw new OperationFailedException(e);
            }
        }

        private char[] generatePassword(int length) {
            char[] password = new char[length];
            for(int i = 0; i < password.length; ++i) {
                password[i] = PASSWORD_ALPHABET[this.random.nextInt(PASSWORD_ALPHABET.length)];
            }
            return password;
        }

        @Override
        public ModelNode getKeyStoreType() {
            return new ModelNode(this.keyStoreType);
        }

        @Override
        public ModelNode getKeyStorePath() {
            return new ModelNode(this.path);
        }

        @Override
        public ModelNode getKeyStorePassword() {
            return new ModelNode(String.valueOf(this.keyStorePassword));
        }

        @Override
        public ModelNode getKeyAlias() {
            return new ModelNode(this.alias);
        }

        @Override
        public ModelNode getKeyPassword() {
            return new ModelNode(String.valueOf(this.keyPassword));
        }
    }

    public EncryptProtocolResourceDefinition(String name, Consumer<ResourceDescriptor> descriptorConfigurator, ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory) {
        super(pathElement(name), descriptorConfigurator.andThen(descriptor -> descriptor
                .addAttributes(Attribute.class)
                .addOperationTranslator(ADD_OPERATION_TRANSLATOR)
                ), address -> new EncryptProtocolConfigurationBuilder<>(address), parentBuilderFactory);
    }
}
