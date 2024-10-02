/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.wildfly.extension.micrometer.MicrometerSubsystemModel.VERSION_1_1_0;
import static org.wildfly.extension.micrometer.MicrometerSubsystemModel.VERSION_2_0_0;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.kohsuke.MetaInfServices;
import org.wildfly.extension.micrometer.otlp.OtlpRegistryDefinitionRegistrar;
import org.wildfly.extension.micrometer.prometheus.PrometheusRegistryDefinitionRegistrar;

@MetaInfServices
public class MicrometerExtensionTransformerRegistration implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return MicrometerConfigurationConstants.NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {
        ChainedTransformationDescriptionBuilder chainedBuilder =
                TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(registration.getCurrentSubsystemVersion());

        // 2.0.0 (WildFly 34) to 1.1.0 (WildFly 33)
        from2(chainedBuilder);

        chainedBuilder.buildAndRegister(registration, new ModelVersion[]{
                VERSION_2_0_0.getVersion(),
                VERSION_1_1_0.getVersion()
        });
    }

    private void from2(ChainedTransformationDescriptionBuilder chainedBuilder) {
        ResourceTransformationDescriptionBuilder builder =
                chainedBuilder.createBuilder(VERSION_2_0_0.getVersion(), VERSION_1_1_0.getVersion());
        builder.addChildRedirection(OtlpRegistryDefinitionRegistrar.PATH, MicrometerSubsystemRegistrar.PATH);
        builder.rejectChildResource(PrometheusRegistryDefinitionRegistrar.PATH);
    }
}
