/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.mod_cluster;

import java.util.EnumSet;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.kohsuke.MetaInfServices;

/**
 * @author Radoslav Husar
 */
@MetaInfServices(ExtensionTransformerRegistration.class)
public class ModClusterExtensionTransformerRegistration implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return ModClusterExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {
        // Register transformers for all but the current model
        for (ModClusterSubsystemModel model : EnumSet.complementOf(EnumSet.of(ModClusterSubsystemModel.CURRENT))) {
            ModelVersion version = model.getVersion();
            TransformationDescription.Tools.register(new ModClusterSubsystemResourceTransformer().apply(version), registration, version);
        }
    }
}
