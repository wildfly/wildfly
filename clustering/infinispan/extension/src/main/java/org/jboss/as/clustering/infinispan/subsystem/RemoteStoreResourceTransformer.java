/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Describes resource transformations for a remote store.
 * @author Paul Ferraro
 */
@Deprecated
public class RemoteStoreResourceTransformer extends StoreResourceTransformer {

    RemoteStoreResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        super(parent.addChildResource(StoreResourceRegistration.REMOTE.getPathElement()));
    }
}
