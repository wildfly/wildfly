/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Describes the resource transformations of a HotRod store.
 * @author Paul Ferraro
 */
public class HotRodStoreResourceTransformer extends StoreResourceTransformer {

    HotRodStoreResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        super(parent.addChildResource(StoreResourceRegistration.HOTROD.getPathElement()));
    }
}
