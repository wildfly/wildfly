/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk;

import static org.wildfly.iiop.openjdk.IIOPExtension.VERSION_3_0;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.kohsuke.MetaInfServices;

/**
 * IIOP Transformers used to transform current model version to legacy model versions for domain mode.
 */
@MetaInfServices(ExtensionTransformerRegistration.class)
public class IIOPTransformers implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return IIOPExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {

        ModelVersion currentModel = subsystemRegistration.getCurrentSubsystemVersion();
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(currentModel);

        // register the transformations required for each legacy version after 3.0.0
        registerTransformers_3_0_0(chainedBuilder.createBuilder(currentModel, VERSION_3_0));

        // create the chained builder which incorporates all transformations
        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[] {
                VERSION_3_0 });
    }

    /*
     * Transformers for changes in model version 4.0.0
     */
    private static void registerTransformers_3_0_0(ResourceTransformationDescriptionBuilder subsystemBuilder) {
        //rejection is attempted - seems the configuration is fine...
        subsystemBuilder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED, "registration-check").end().
                setCustomResourceTransformer(new ResourceTransformer() {
            @Override
            public void transformResource(ResourceTransformationContext resourceTransformationContext, PathAddress pathAddress, Resource resource) throws OperationFailedException {
                //... but this code is not executed
            }
        });
    }
}

