/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.mod_cluster;

import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * Transformer logic for {@link DynamicLoadProviderResourceDefinition}.
 *
 * @author Radoslav Husar
 */
public class DynamicLoadProviderResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder builder;

    public DynamicLoadProviderResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.builder = parent.addChildResource(DynamicLoadProviderResourceDefinition.PATH);
    }

    @Override
    public void accept(ModelVersion version) {
        if (ModClusterSubsystemModel.VERSION_7_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(-1)), DynamicLoadProviderResourceDefinition.Attribute.INITIAL_LOAD.getDefinition())
                    .addRejectCheck(RejectAttributeChecker.DEFINED, DynamicLoadProviderResourceDefinition.Attribute.INITIAL_LOAD.getDefinition())
                    .end();
        }

        //new LoadMetricResourceTransformer(this.builder).accept(version);
        //new CustomLoadMetricResourceTransformer(this.builder).accept(version);
    }
}
