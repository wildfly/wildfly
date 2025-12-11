/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Describes resource transformations for a segmented cache configuration.
 * @author Paul Ferraro
 */
public class SegmentedCacheResourceTransformer extends SharedStateCacheResourceTransformer {

    SegmentedCacheResourceTransformer(ResourceTransformationDescriptionBuilder builder) {
        super(builder);
    }

    @Override
    public void accept(ModelVersion version) {
        super.accept(version);
        if (InfinispanSubsystemModel.VERSION_21_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                .setValueConverter(AttributeConverter.DEFAULT_VALUE, SegmentedCacheResourceDefinitionRegistrar.SEGMENTS)
                .end();
        }
    }
}
