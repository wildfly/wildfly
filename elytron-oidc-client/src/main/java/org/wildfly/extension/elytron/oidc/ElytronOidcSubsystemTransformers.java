/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.wildfly.extension.elytron.oidc.ElytronOidcClientSubsystemModel.VERSION_1_0_0;
import static org.wildfly.extension.elytron.oidc.ElytronOidcClientSubsystemModel.VERSION_2_0_0;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.SECURE_SERVER;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
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
        // 3.0.0 (WildFly 32) to 2.0.0 (WildFly 29)
//        from3(chainedBuilder);
        // 2.0.0 (WildFly 29) to 1.0.0 (WildFly 28)
        from2(chainedBuilder);

        chainedBuilder.buildAndRegister(registration, new ModelVersion[] { VERSION_1_0_0.getVersion() });
    }

    private static void from2(ChainedTransformationDescriptionBuilder chainedBuilder) {
        ResourceTransformationDescriptionBuilder builder = chainedBuilder.createBuilder(VERSION_2_0_0.getVersion(), VERSION_1_0_0.getVersion());
        builder.rejectChildResource(PathElement.pathElement(SECURE_SERVER));
    }
    // TODO: Figure out how to handle the transformer with a preview level attribute being added to the subsystem
//    private static void from3(ChainedTransformationDescriptionBuilder chainedBuilder) {
//        ResourceTransformationDescriptionBuilder builder = chainedBuilder.createBuilder(VERSION_3_0_0.getVersion(), VERSION_2_0_0.getVersion());
//        builder.addChildResource(PathElement.pathElement(SECURE_SERVER))
//                .getAttributeBuilder();
//
//        builder.addChildResource(PathElement.pathElement(SECURE_DEPLOYMENT))
//                .getAttributeBuilder();
//    }

}
