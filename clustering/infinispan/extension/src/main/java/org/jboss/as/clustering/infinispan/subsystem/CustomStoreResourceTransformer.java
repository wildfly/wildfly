/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Describes the resource transformations of a custom cache store resource.
 * @author Paul Ferraro
 */
public class CustomStoreResourceTransformer extends StoreResourceTransformer {

    CustomStoreResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        super(parent.addChildResource(StoreResourceRegistration.CUSTOM.getPathElement()));
    }
}
