/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import java.util.EnumSet;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.kohsuke.MetaInfServices;

/**
 * Transformer registration for the distributable-ejb subsystem.
 * @author Radoslav Husar
 */
@MetaInfServices(ExtensionTransformerRegistration.class)
public class DistributableEjbExtensionTransformerRegistration implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return DistributableEjbSubsystemResourceDefinitionRegistrar.REGISTRATION.getName();
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {
        // Register transformers for all but the current model
        for (DistributableEjbSubsystemModel model : EnumSet.complementOf(EnumSet.of(DistributableEjbSubsystemModel.CURRENT))) {
            ModelVersion version = model.getVersion();
            TransformationDescription.Tools.register(new DistributableEjbSubsystemResourceTransformer().apply(version).build(), registration, version);
        }
    }
}
