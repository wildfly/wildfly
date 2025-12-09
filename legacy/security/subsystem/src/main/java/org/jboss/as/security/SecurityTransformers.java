/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.security;

import static org.jboss.as.security.Constants.ORG_PICKETBOX;
import static org.jboss.as.security.MappingProviderModuleDefinition.MODULE;
import static org.jboss.as.security.MappingProviderModuleDefinition.PATH_PROVIDER_MODULE;
import static org.jboss.as.security.SecuritySubsystemRootResourceDefinition.INITIALIZE_JACC;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
public class SecurityTransformers implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return SecurityExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        // only register transformers for model version 1.3.0 (EAP 6.2+).
        registerTransformers_1_3_0(subsystemRegistration);
    }

    private void registerTransformers_1_3_0(SubsystemTransformerRegistration subsystemRegistration) {
        ResourceTransformationDescriptionBuilder builder = ResourceTransformationDescriptionBuilder.Factory.createSubsystemInstance();
        builder.rejectChildResource(PathElement.pathElement(Constants.ELYTRON_REALM));
        builder.rejectChildResource(PathElement.pathElement(Constants.ELYTRON_KEY_STORE));
        builder.rejectChildResource(PathElement.pathElement(Constants.ELYTRON_TRUST_STORE));
        builder.rejectChildResource(PathElement.pathElement(Constants.ELYTRON_KEY_MANAGER));
        builder.rejectChildResource(PathElement.pathElement(Constants.ELYTRON_TRUST_MANAGER));
        builder.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, INITIALIZE_JACC)
                .addRejectCheck(RejectAttributeChecker.DEFINED, INITIALIZE_JACC);


        builder
                .addChildResource(SecurityExtension.SECURITY_DOMAIN_PATH)
                .addChildResource(SecurityExtension.PATH_AUDIT_CLASSIC)
                .addChildResource(PATH_PROVIDER_MODULE)
                .getAttributeBuilder()
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(ORG_PICKETBOX)), MODULE)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, MODULE).end();

        TransformationDescription.Tools.register(builder.build(), subsystemRegistration, ModelVersion.create(1, 3, 0));
    }
}
