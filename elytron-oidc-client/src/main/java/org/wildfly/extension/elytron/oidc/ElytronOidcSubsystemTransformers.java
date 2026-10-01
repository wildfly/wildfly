/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.wildfly.extension.elytron.oidc.ElytronOidcClientSubsystemModel.VERSION_1_0_0;
import static org.wildfly.extension.elytron.oidc.ElytronOidcClientSubsystemModel.VERSION_2_0_0;
import static org.wildfly.extension.elytron.oidc.ElytronOidcClientSubsystemModel.VERSION_3_0_0;
import static org.wildfly.extension.elytron.oidc.ElytronOidcClientSubsystemModel.VERSION_4_0_0;
import static org.wildfly.extension.elytron.oidc.ElytronOidcClientSubsystemModel.VERSION_5_0_0;
import static org.wildfly.extension.elytron.oidc.ElytronOidcClientSubsystemModel.VERSION_6_0_0;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.PROVIDER;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.REALM;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.SECURE_SERVER;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.PROVIDER_JWT_CLAIMS_TYP;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.BACK_CHANNEL_LOGOUT_SESSION_INVALIDATION_LIMIT;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.LOGOUT_CALLBACK_PATH;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.LOGOUT_PATH;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.LOGOUT_SESSION_REQUIRED;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.POST_LOGOUT_REDIRECT_URI;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

public class ElytronOidcSubsystemTransformers implements ExtensionTransformerRegistration {

    private static final AttributeDefinition[] LOGOUT_ATTRIBUTES = {
            LOGOUT_PATH,
            LOGOUT_CALLBACK_PATH,
            POST_LOGOUT_REDIRECT_URI,
            LOGOUT_SESSION_REQUIRED,
            BACK_CHANNEL_LOGOUT_SESSION_INVALIDATION_LIMIT,
            PROVIDER_JWT_CLAIMS_TYP
    };

    @Override
    public String getSubsystemName() {
        return ElytronOidcExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {

        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(registration.getCurrentSubsystemVersion());

        // 6.0.0 (WildFly 41) to 5.0.0 (WildFly 40)
        from6(chainedBuilder);
        // 5.0.0 (WildFly 40) to 4.0.0 (WildFly 33)
        from5(chainedBuilder);
        // 4.0.0 (WildFly 33) to 3.0.0 (WildFly 32)
        from4(chainedBuilder);
        // 3.0.0 (WildFly 32) to 2.0.0 (WildFly 29)
        from3(chainedBuilder);
        // 2.0.0 (WildFly 29) to 1.0.0 (WildFly 28)
        from2(chainedBuilder);

        chainedBuilder.buildAndRegister(registration, new ModelVersion[] {
                VERSION_5_0_0.getVersion(), VERSION_4_0_0.getVersion(),
                VERSION_3_0_0.getVersion(), VERSION_2_0_0.getVersion(),
                VERSION_1_0_0.getVersion() });
    }

    private static void from2(ChainedTransformationDescriptionBuilder chainedBuilder) {
        ResourceTransformationDescriptionBuilder builder = chainedBuilder.createBuilder(VERSION_2_0_0.getVersion(), VERSION_1_0_0.getVersion());
        builder.rejectChildResource(PathElement.pathElement(SECURE_SERVER));
    }

    private static void from3(ChainedTransformationDescriptionBuilder chainedBuilder) {
        // No default stability changes in version 3.0.0
    }

    private static void from4(ChainedTransformationDescriptionBuilder chainedBuilder) {
        // No default stability changes in version 4.0.0
    }

    private static void from5(ChainedTransformationDescriptionBuilder chainedBuilder) {
        // No default stability changes in version 5.0.0
    }

    private static void from6(ChainedTransformationDescriptionBuilder chainedBuilder) {
        discardLogoutAttributes(chainedBuilder.createBuilder(VERSION_6_0_0.getVersion(), VERSION_5_0_0.getVersion()));
    }

    private static void discardLogoutAttributes(ResourceTransformationDescriptionBuilder subsystemBuilder) {
        discardLogoutAttributesOnResource(subsystemBuilder.addChildResource(PathElement.pathElement(REALM)));
        discardLogoutAttributesOnResource(subsystemBuilder.addChildResource(PathElement.pathElement(PROVIDER)));
        discardLogoutAttributesOnResource(subsystemBuilder.addChildResource(PathElement.pathElement(SECURE_DEPLOYMENT)));
        discardLogoutAttributesOnResource(subsystemBuilder.addChildResource(PathElement.pathElement(SECURE_SERVER)));
    }

    private static void discardLogoutAttributesOnResource(ResourceTransformationDescriptionBuilder resourceBuilder) {
        resourceBuilder.getAttributeBuilder().setDiscard(DiscardAttributeChecker.UNDEFINED, LOGOUT_ATTRIBUTES);
    }
}
