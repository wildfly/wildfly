/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.wildfly.extension.elytron.oidc.ElytronOidcClientSubsystemModel.VERSION_1_0_0;
import static org.wildfly.extension.elytron.oidc.ElytronOidcClientSubsystemModel.VERSION_2_0_0;
import static org.wildfly.extension.elytron.oidc.ElytronOidcClientSubsystemModel.VERSION_3_0_0;
import static org.wildfly.extension.elytron.oidc.ElytronOidcClientSubsystemModel.VERSION_4_0_0;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.PROVIDER;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.REALM;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.SECURE_SERVER;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

public class ElytronOidcSubsystemTransformers implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return ElytronOidcExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {

        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(registration.getCurrentSubsystemVersion());

        // 4.0.0 (WildFly 33) to 3.0.0 (WildFly 32)
        from4(chainedBuilder);
        // 3.0.0 (WildFly 32) to 2.0.0 (WildFly 29)
        from3(chainedBuilder);
        // 2.0.0 (WildFly 29) to 1.0.0 (WildFly 28)
        from2(chainedBuilder);

        chainedBuilder.buildAndRegister(registration, new ModelVersion[] { VERSION_3_0_0.getVersion(), VERSION_2_0_0.getVersion(), VERSION_1_0_0.getVersion() });
    }

    private static void from2(ChainedTransformationDescriptionBuilder chainedBuilder) {
        ResourceTransformationDescriptionBuilder builder = chainedBuilder.createBuilder(VERSION_2_0_0.getVersion(), VERSION_1_0_0.getVersion());
        builder.rejectChildResource(PathElement.pathElement(SECURE_SERVER));
    }

    private static void from3(ChainedTransformationDescriptionBuilder chainedBuilder) {
        ResourceTransformationDescriptionBuilder builder = chainedBuilder.createBuilder(VERSION_3_0_0.getVersion(), VERSION_2_0_0.getVersion());

        builder.addChildResource(PathElement.pathElement(SECURE_SERVER))
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, SecureDeploymentDefinition.SCOPE)
                .setDiscard(DiscardAttributeChecker.ALWAYS, SecureDeploymentDefinition.SCOPE)
                .end();

        builder.addChildResource(PathElement.pathElement(SECURE_DEPLOYMENT))
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, SecureDeploymentDefinition.SCOPE)
                .setDiscard(DiscardAttributeChecker.ALWAYS, SecureDeploymentDefinition.SCOPE)
                .end();
    }

    private static void from4(ChainedTransformationDescriptionBuilder chainedBuilder) {
        ResourceTransformationDescriptionBuilder builder = chainedBuilder.createBuilder(VERSION_4_0_0.getVersion(), VERSION_3_0_0.getVersion());
        builder.addChildResource(PathElement.pathElement(SECURE_SERVER))
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.AUTHENTICATION_REQUEST_FORMAT)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.AUTHENTICATION_REQUEST_FORMAT)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ALG_VALUE)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ALG_VALUE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ENC_VALUE)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ENC_VALUE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_ALGORITHM)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_ALGORITHM)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_ALIAS)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_ALIAS)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_PASSWORD)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_PASSWORD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_FILE)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_FILE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE)
                .end();

        builder.addChildResource(PathElement.pathElement(SECURE_DEPLOYMENT))
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.AUTHENTICATION_REQUEST_FORMAT)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.AUTHENTICATION_REQUEST_FORMAT)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ALG_VALUE)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ALG_VALUE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ENC_VALUE)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ENC_VALUE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_ALGORITHM)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_ALGORITHM)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_ALIAS)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_ALIAS)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_PASSWORD)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_PASSWORD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_FILE)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_FILE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE)
                .end();

        builder.addChildResource(PathElement.pathElement(REALM))
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.AUTHENTICATION_REQUEST_FORMAT)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.AUTHENTICATION_REQUEST_FORMAT)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ALG_VALUE)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ALG_VALUE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ENC_VALUE)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ENC_VALUE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_ALGORITHM)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_ALGORITHM)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_ALIAS)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_ALIAS)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_PASSWORD)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_PASSWORD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_FILE)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_FILE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE)
                .end();

        builder.addChildResource(PathElement.pathElement(PROVIDER))
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.AUTHENTICATION_REQUEST_FORMAT)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.AUTHENTICATION_REQUEST_FORMAT)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ALG_VALUE)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ALG_VALUE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ENC_VALUE)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ENC_VALUE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_ALGORITHM)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_ALGORITHM)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_ALIAS)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_ALIAS)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_PASSWORD)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_PASSWORD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_FILE)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_FILE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE)
                .setDiscard(DiscardAttributeChecker.ALWAYS, ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE)
                .end();
    }

}
