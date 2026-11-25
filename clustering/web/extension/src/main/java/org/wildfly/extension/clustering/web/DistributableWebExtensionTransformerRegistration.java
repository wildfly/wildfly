/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.EnumSet;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.kohsuke.MetaInfServices;

/**
 * Registers transformers for the distributable-web subsystem.
 * @author Paul Ferraro
 */
@MetaInfServices(ExtensionTransformerRegistration.class)
public class DistributableWebExtensionTransformerRegistration implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return DistributableWebSubsystemResourceDefinitionRegistrar.REGISTRATION.getName();
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {
        // Register transformers for all but the current model
        for (DistributableWebSubsystemModel model : EnumSet.complementOf(EnumSet.of(DistributableWebSubsystemModel.CURRENT))) {
            ModelVersion version = model.getVersion();
            TransformationDescription transformation = new DistributableWebSubsystemResourceTransformer().apply(version).build();
            TransformationDescription.Tools.register(transformation, registration, version);
        }
    }
}
