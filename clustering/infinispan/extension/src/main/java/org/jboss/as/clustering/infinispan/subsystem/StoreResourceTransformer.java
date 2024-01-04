/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transformers for cache store resources.
 * @author Paul Ferraro
 */
public class StoreResourceTransformer implements Consumer<ModelVersion> {

    final ResourceTransformationDescriptionBuilder builder;

    StoreResourceTransformer(ResourceTransformationDescriptionBuilder builder) {
        this.builder = builder;
    }

    @Override
    public void accept(ModelVersion version) {
        if (InfinispanSubsystemModel.VERSION_16_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, StoreResourceDefinition.Attribute.SEGMENTED.getDefinition())
                .addRejectCheck(RejectAttributeChecker.DEFINED, StoreResourceDefinition.Attribute.SEGMENTED.getDefinition())
                .setValueConverter(AttributeConverter.DEFAULT_VALUE, StoreResourceDefinition.Attribute.PASSIVATION.getDefinition())
                .setValueConverter(AttributeConverter.DEFAULT_VALUE, StoreResourceDefinition.Attribute.PURGE.getDefinition())
                .end();
        }
    }
}
