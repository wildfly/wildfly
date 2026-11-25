/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Describes resource transformations for a replicated cache.
 * @author Paul Ferraro
 */
public class ReplicatedCacheResourceTransformer extends SharedStateCacheResourceTransformer {

    ReplicatedCacheResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        super(parent.addChildResource(CacheResourceRegistration.REPLICATED.getPathElement()));
    }

    @Override
    public void accept(ModelVersion version) {
        super.accept(version);

        if (InfinispanSubsystemModel.VERSION_22_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, SegmentedCacheResourceDefinitionRegistrar.SEGMENTS)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, SegmentedCacheResourceDefinitionRegistrar.SEGMENTS)
                    .end();
        }
    }
}
