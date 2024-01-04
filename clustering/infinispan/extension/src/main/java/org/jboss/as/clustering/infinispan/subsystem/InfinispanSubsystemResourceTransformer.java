/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Function;

import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceTransformer;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

/**
 * Transformer for the Infinispan subsystem resource.
 * @author Paul Ferraro
 */
public class InfinispanSubsystemResourceTransformer implements Function<ModelVersion, TransformationDescription> {

    private final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

    @Override
    public TransformationDescription apply(ModelVersion version) {

        new CacheContainerResourceTransformer(this.builder).accept(version);
        new RemoteCacheContainerResourceTransformer(this.builder).accept(version);

        return this.builder.build();
    }
}
