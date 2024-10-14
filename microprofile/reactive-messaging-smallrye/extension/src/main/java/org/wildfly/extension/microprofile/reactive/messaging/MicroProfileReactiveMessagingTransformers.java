/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.reactive.messaging;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

public class MicroProfileReactiveMessagingTransformers implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return MicroProfileReactiveMessagingExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystem) {

        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory
                .createChainedSubystemInstance(subsystem.getCurrentSubsystemVersion());

        // Differences between the current version (2.0.0) and 1.0.0
        ResourceTransformationDescriptionBuilder builder_1_0_0 = chainedBuilder.createBuilder(subsystem.getCurrentSubsystemVersion(), MicroProfileReactiveMessagingExtension.VERSION_1_0_0);
        builder_1_0_0.rejectChildResource(ConnectorOpenTelemetryTracingResourceDefinition.PATH);

        chainedBuilder.buildAndRegister(subsystem, new ModelVersion[]{MicroProfileReactiveMessagingExtension.VERSION_1_0_0});
    }
}
