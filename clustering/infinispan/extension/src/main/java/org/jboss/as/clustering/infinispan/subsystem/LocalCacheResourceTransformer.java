/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * @author Paul Ferraro
 */
public class LocalCacheResourceTransformer extends CacheResourceTransformer {

    LocalCacheResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        super(parent.addChildResource(LocalCacheResourceDefinition.WILDCARD_PATH));
    }
}
