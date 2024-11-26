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
import org.kohsuke.MetaInfServices;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:ema@redhat.com>Jim Ma</a>
 */
@MetaInfServices
public class ResteasyExtensionTransformerRegistration implements ExtensionTransformerRegistration {

    private static final ModelVersion VERSION_4_1_0 = ModelVersion.create(4, 1, 0);
    private static final ModelVersion VERSION_4_0_0 = ModelVersion.create(4, 0, 0);
    private static final ModelVersion VERSION_3_0_0 = ModelVersion.create(3, 0, 0);

    @Override
    public String getSubsystemName() {
        return JaxrsExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(final SubsystemTransformerRegistration subsystemRegistration) {
        ChainedTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(subsystemRegistration.getCurrentSubsystemVersion());
        registerV31Transformers(builder.createBuilder(VERSION_4_1_0, VERSION_4_0_0));
        registerV3Transformers(builder.createBuilder(VERSION_4_0_0, VERSION_3_0_0));
        builder.buildAndRegister(subsystemRegistration, new ModelVersion[] {VERSION_4_1_0, VERSION_4_0_0, VERSION_3_0_0});
    }

    private static void registerV31Transformers(ResourceTransformationDescriptionBuilder subsystem) {
        subsystem.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, JaxrsAttribute.RESTEASY_PATCHFILTER_DISABLED)
                .addRejectCheck(RejectAttributeChecker.DEFINED, JaxrsAttribute.RESTEASY_PATCHFILTER_DISABLED);
    }

    private static void registerV3Transformers(ResourceTransformationDescriptionBuilder subsystem) {
        subsystem.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, JaxrsAttribute.TRACING_TYPE, JaxrsAttribute.TRACING_THRESHOLD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, JaxrsAttribute.TRACING_TYPE, JaxrsAttribute.TRACING_THRESHOLD);
    }
}
