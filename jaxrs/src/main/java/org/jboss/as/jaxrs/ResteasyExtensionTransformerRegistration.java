/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jaxrs;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.as.jaxrs.JaxrsExtension.JaxrsSubsystemModel;
import org.kohsuke.MetaInfServices;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MetaInfServices
public class ResteasyExtensionTransformerRegistration implements ExtensionTransformerRegistration {

    private static final ModelVersion VERSION_3_0_0 = ModelVersion.create(3, 0, 0);

    @Override
    public String getSubsystemName() {
        return JaxrsExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(final SubsystemTransformerRegistration subsystemRegistration) {
        ChainedTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(subsystemRegistration.getCurrentSubsystemVersion());

        registerV5Transformers(builder.createBuilder(JaxrsSubsystemModel.CURRENT.getVersion(), JaxrsSubsystemModel.VERSION_5_0_0.getVersion()));
        registerV4Transformers(builder.createBuilder(JaxrsSubsystemModel.VERSION_5_0_0.getVersion(), JaxrsSubsystemModel.VERSION_4_0_0.getVersion()));
        registerV3Transformers(builder.createBuilder(JaxrsSubsystemModel.VERSION_4_0_0.getVersion(), VERSION_3_0_0));

        builder.buildAndRegister(subsystemRegistration, new ModelVersion[] {VERSION_3_0_0, JaxrsSubsystemModel.VERSION_4_0_0.getVersion(),  JaxrsSubsystemModel.VERSION_5_0_0.getVersion(), JaxrsSubsystemModel.CURRENT.getVersion()});
    }

    private static void registerV3Transformers(ResourceTransformationDescriptionBuilder subsystem) {
        subsystem.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, JaxrsAttribute.TRACING_TYPE, JaxrsAttribute.TRACING_THRESHOLD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, JaxrsAttribute.TRACING_TYPE, JaxrsAttribute.TRACING_THRESHOLD);
    }

    private static void registerV4Transformers(ResourceTransformationDescriptionBuilder subsystem) {
        subsystem.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, JaxrsAttribute.RESTEASY_PATCHFILTER_DISABLED)
                .addRejectCheck(RejectAttributeChecker.DEFINED, JaxrsAttribute.RESTEASY_PATCHFILTER_DISABLED);
    }

    private static void registerV5Transformers(ResourceTransformationDescriptionBuilder subsystem) {
        subsystem.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE,
                        JaxrsAttribute.RESTEASY_ORIGINAL_WEBAPPLICATIONEXCEPTION_BEHAVIOR
                )
                .addRejectCheck(RejectAttributeChecker.DEFINED,
                        JaxrsAttribute.RESTEASY_ORIGINAL_WEBAPPLICATIONEXCEPTION_BEHAVIOR
                );
    }
}
