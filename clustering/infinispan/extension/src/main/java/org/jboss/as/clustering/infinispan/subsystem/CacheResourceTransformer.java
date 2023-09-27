/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transformers for cache resources.
 * @author Paul Ferraro
 */
public class CacheResourceTransformer implements Consumer<ModelVersion> {

    final ResourceTransformationDescriptionBuilder builder;

    CacheResourceTransformer(ResourceTransformationDescriptionBuilder builder) {
        this.builder = builder;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void accept(ModelVersion version) {
        new TransactionResourceTransformer(this.builder).accept(version);
        new CustomStoreResourceTransformer(this.builder).accept(version);
        new FileStoreResourceTransformer(this.builder).accept(version);
        new HotRodStoreResourceTransformer(this.builder).accept(version);
        new JDBCStoreResourceTransformer(this.builder).accept(version);
        new RemoteStoreResourceTransformer(this.builder).accept(version);
    }
}
