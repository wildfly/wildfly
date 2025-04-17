/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Registers resource transformations for a shared state cache configuration.
 * @author Paul Ferraro
 */
public class SharedStateCacheResourceTransformer extends ClusteredCacheResourceTransformer {

    SharedStateCacheResourceTransformer(ResourceTransformationDescriptionBuilder builder) {
        super(builder);
    }

    @Override
    public void accept(ModelVersion version) {
        super.accept(version);

        new PartitionHandlingResourceTransformer(this.builder).accept(version);
    }
}
