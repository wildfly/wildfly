/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.function.Function;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

/**
 * Describes resource transformations for the subsystem subsystem.
 * @author Paul Ferraro
 */
public class SingletonSubsystemResourceTransformer implements Function<ModelVersion, TransformationDescription> {

    private final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

    @Override
    public TransformationDescription apply(ModelVersion version) {
        new SingletonPolicyResourceTransformer(this.builder).accept(version);

        return this.builder.build();
    }
}
