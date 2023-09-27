/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.mod_cluster;

import java.util.function.Function;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

/**
 * Transformer logic for {@link ModClusterSubsystemResourceDefinition}.
 *
 * @author Radoslav Husar
 */
public class ModClusterSubsystemResourceTransformer implements Function<ModelVersion, TransformationDescription> {

    private final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

    @Override
    public TransformationDescription apply(ModelVersion version) {

        new ProxyConfigurationResourceTransformer(this.builder).accept(version);

        return this.builder.build();
    }
}
