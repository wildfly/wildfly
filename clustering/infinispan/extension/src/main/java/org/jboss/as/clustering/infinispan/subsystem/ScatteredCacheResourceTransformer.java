/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * @author Paul Ferraro
 */
@Deprecated
public class ScatteredCacheResourceTransformer extends SegmentedCacheResourceTransformer {

    ScatteredCacheResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        super(parent.addChildResource(ScatteredCacheResourceDefinition.WILDCARD_PATH));
    }
}
