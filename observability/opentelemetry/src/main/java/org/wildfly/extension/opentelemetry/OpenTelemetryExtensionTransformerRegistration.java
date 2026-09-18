/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.opentelemetry;

import static org.wildfly.extension.opentelemetry.OpenTelemetrySubsystemModel.VERSION_1_0_0;
import static org.wildfly.extension.opentelemetry.OpenTelemetrySubsystemModel.VERSION_1_1_0;

import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.kohsuke.MetaInfServices;

@MetaInfServices
public class OpenTelemetryExtensionTransformerRegistration implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return OpenTelemetryConfigurationConstants.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {
        ChainedTransformationDescriptionBuilder builder =
                TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(registration.getCurrentSubsystemVersion());

        builder.createBuilder(OpenTelemetrySubsystemModel.VERSION_1_2_0.getVersion(), VERSION_1_1_0.getVersion());
        registerV_1_1_Transformers(builder.createBuilder(VERSION_1_1_0.getVersion(), VERSION_1_0_0.getVersion()));

        builder.buildAndRegister(registration, new org.jboss.as.controller.ModelVersion[]{
                VERSION_1_1_0.getVersion(), VERSION_1_0_0.getVersion()});
    }

    private void registerV_1_1_Transformers(ResourceTransformationDescriptionBuilder builder) {
        builder.getAttributeBuilder()
                .setValueConverter(AttributeConverter.DEFAULT_VALUE, OpenTelemetrySubsystemRegistrar.EXPORTER)
                .end();
    }
}
