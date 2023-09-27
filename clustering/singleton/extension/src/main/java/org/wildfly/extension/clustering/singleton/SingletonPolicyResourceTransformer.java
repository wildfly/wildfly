/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transforms the singleton policy resource.
 * @author Paul Ferraro
 */
public class SingletonPolicyResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder builder;

    SingletonPolicyResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.builder = parent.addChildResource(SingletonPolicyResourceDefinition.WILDCARD_PATH);
    }

    @Override
    public void accept(ModelVersion version) {
        if (SingletonSubsystemModel.VERSION_3_0_0.requiresTransformation(version)) {
            this.builder.discardChildResource(SingletonDeploymentResourceDefinition.WILDCARD_PATH);
            this.builder.discardChildResource(SingletonServiceResourceDefinition.WILDCARD_PATH);
        }
    }
}
