/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Describes resource transformations for a replicated cache.
 * @author Paul Ferraro
 */
public class ReplicatedCacheResourceTransformer extends SharedStateCacheResourceTransformer {

    ReplicatedCacheResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        super(parent.addChildResource(CacheResourceRegistration.REPLICATED.getPathElement()));
    }
}
