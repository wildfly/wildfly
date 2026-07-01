/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

/**
 * Transformers for EE Subsystem wrt Jakarta Concurrency resources.
 * @author emartins
 */
public class ConcurrentTransformers implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return EeExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        final ModelVersion currentModel = subsystemRegistration.getCurrentSubsystemVersion();
        final ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(currentModel);
        chainedBuilder.createBuilder(subsystemRegistration.getCurrentSubsystemVersion(), EESubsystemModel.Version.v7_0_0).build();
        registerTransformersFrom700to600(chainedBuilder.createBuilder(EESubsystemModel.Version.v7_0_0, EESubsystemModel.Version.v6_0_0));
        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[] {
                EESubsystemModel.Version.v6_0_0
        });
    }

    /*
     Transformations from 7.0.0 to 6.0.0
     */
    private static void registerTransformersFrom700to600(ResourceTransformationDescriptionBuilder builder) {
        builder.addChildResource(PathElement.pathElement(EESubsystemModel.MANAGED_EXECUTOR_SERVICE))
                .getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.ALWAYS, "virtual");
        builder.addChildResource(PathElement.pathElement(EESubsystemModel.MANAGED_SCHEDULED_EXECUTOR_SERVICE))
                .getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.ALWAYS, "virtual");
        builder.addChildResource(PathElement.pathElement(EESubsystemModel.MANAGED_THREAD_FACTORY))
                .getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.ALWAYS, "virtual");
    }
}
