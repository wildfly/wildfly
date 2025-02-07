/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.kohsuke.MetaInfServices;
import org.wildfly.extension.micrometer.otlp.OtlpRegistryDefinitionRegistrar;

@MetaInfServices
public class MicrometerExtensionTransformerRegistration implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return MicrometerConfigurationConstants.NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {
        ChainedTransformationDescriptionBuilder builder =
                TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(
                        registration.getCurrentSubsystemVersion());

        registerV_2_0_Transformers(builder.createBuilder(MicrometerSubsystemModel.VERSION_2_0_0.getVersion(),
                MicrometerSubsystemModel.VERSION_1_1_0.getVersion()));
    }

    private void registerV_2_0_Transformers(ResourceTransformationDescriptionBuilder builder) {
        builder.addChildRedirection(OtlpRegistryDefinitionRegistrar.PATH, MicrometerSubsystemRegistrar.PATH);
    }
}
