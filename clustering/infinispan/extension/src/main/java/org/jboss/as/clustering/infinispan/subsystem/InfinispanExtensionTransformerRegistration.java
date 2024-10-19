/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.kohsuke.MetaInfServices;

/**
 * Registers resource transformations for the extension providing the Infinispan subsystem.
 * @author Paul Ferraro
 */
@MetaInfServices(ExtensionTransformerRegistration.class)
public class InfinispanExtensionTransformerRegistration implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return InfinispanSubsystemResourceDefinitionRegistrar.REGISTRATION.getName();
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {
        // Register transformers for all but the current model
        for (InfinispanSubsystemModel model : EnumSet.complementOf(EnumSet.of(InfinispanSubsystemModel.CURRENT))) {
            ModelVersion version = model.getVersion();
            TransformationDescription.Tools.register(new InfinispanSubsystemResourceTransformer().apply(version), registration, version);
        }
    }
}
