/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.coordinator;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.kohsuke.MetaInfServices;

/**
 * Registers model transformers for the MicroProfile LRA coordinator subsystem.
 * @author Marco Sappé Griot
 */
@MetaInfServices
public class MicroProfileLRACoordinatorTransformers implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return MicroProfileLRACoordinatorExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {
        ChainedTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(registration.getCurrentSubsystemVersion());

        registerTransformers_1_0_0(builder.createBuilder(MicroProfileLRACoordinatorSubsystemModel.VERSION_2_0_0.getVersion(), MicroProfileLRACoordinatorSubsystemModel.VERSION_1_0_0.getVersion()));

        builder.buildAndRegister(registration, new ModelVersion[]{ MicroProfileLRACoordinatorSubsystemModel.VERSION_1_0_0.getVersion() });
    }

    @SuppressWarnings("unused")
    private void registerTransformers_1_0_0(ResourceTransformationDescriptionBuilder builder) {
        // Version 2.0.0 turns the 'server' and 'host' attributes into capability references and
        // removes their default values and expression support. These are metadata/validation changes
        // only; the management model structure and value space are unchanged, so no attribute
        // transformation is required. A 2.0.0 model can never hold an expression for these attributes,
        // and an undefined value transforms cleanly to a 1.0.0 host (which applies its own default).
    }
}
