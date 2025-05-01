/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Describes the resource transformations of a file store.
 * @author Paul Ferraro
 */
public class FileStoreResourceTransformer extends StoreResourceTransformer {

    FileStoreResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        super(parent.addChildResource(StoreResourceRegistration.FILE.getPathElement()));
    }
}
