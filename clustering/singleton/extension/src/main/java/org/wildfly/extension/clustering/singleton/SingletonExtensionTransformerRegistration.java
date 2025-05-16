/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.EnumSet;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.kohsuke.MetaInfServices;

/**
 * Transformer registration for the singleton subsystem.
 * @author Paul Ferraro
 */
@MetaInfServices(ExtensionTransformerRegistration.class)
public class SingletonExtensionTransformerRegistration implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return SingletonSubsystemResourceDefinitionRegistrar.REGISTRATION.getName();
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {
        // Register transformers for all but the current model
        for (SingletonSubsystemModel model : EnumSet.complementOf(EnumSet.of(SingletonSubsystemModel.CURRENT))) {
            ModelVersion version = model.getVersion();
            TransformationDescription.Tools.register(new SingletonSubsystemResourceTransformer().apply(version), registration, version);
        }
    }
}
