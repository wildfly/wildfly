/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * @author Paul Ferraro
 */
public class ClusteredCacheResourceTransformer extends CacheResourceTransformer {

    ClusteredCacheResourceTransformer(ResourceTransformationDescriptionBuilder builder) {
        super(builder);
    }

    @Override
    public void accept(ModelVersion version) {
        if (InfinispanSubsystemModel.VERSION_17_1_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                .setValueConverter(AttributeConverter.DEFAULT_VALUE, ClusteredCacheResourceDefinition.Attribute.REMOTE_TIMEOUT.getDefinition())
                .end();
        }
        super.accept(version);
    }
}
