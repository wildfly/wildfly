/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Describes resource transformations for an invalidation cache.
 * @author Paul Ferraro
 */
public class InvalidationCacheResourceTransformer extends ClusteredCacheResourceTransformer {

    InvalidationCacheResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        super(parent.addChildResource(CacheResourceRegistration.INVALIDATION.getPathElement()));
    }
}
