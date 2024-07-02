/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.function.Consumer;

import org.jboss.as.clustering.infinispan.subsystem.InfinispanSubsystemModel;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transformer for remote cluster resources.
 * @author Paul Ferraro
 */
public class RemoteClusterResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder builder;

    RemoteClusterResourceTransformer(ResourceTransformationDescriptionBuilder builder) {
        this.builder = builder.addChildResource(RemoteClusterResourceDefinition.WILDCARD_PATH);
    }

    @Override
    public void accept(ModelVersion version) {
        if (InfinispanSubsystemModel.VERSION_19_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED, RemoteClusterResourceDefinition.Attribute.DOMAIN.getDefinition())
                .addRejectCheck(RejectAttributeChecker.DEFINED, RemoteClusterResourceDefinition.Attribute.DOMAIN.getDefinition())
                .end();
        }
    }
}
