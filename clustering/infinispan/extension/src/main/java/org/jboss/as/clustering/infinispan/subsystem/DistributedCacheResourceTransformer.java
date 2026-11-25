/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Describes the resource transformations for a distributed cache.
 * @author Paul Ferraro
 */
public class DistributedCacheResourceTransformer extends SegmentedCacheResourceTransformer {

    DistributedCacheResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        super(parent.addChildResource(CacheResourceRegistration.DISTRIBUTED.getPathElement()));
    }
}
