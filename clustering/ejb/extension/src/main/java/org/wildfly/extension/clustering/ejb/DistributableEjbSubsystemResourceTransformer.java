/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import java.util.function.Function;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

/**
 * Transformer for the distributable-ejb subsystem.
 * @author Radoslav Husar
 */
public class DistributableEjbSubsystemResourceTransformer implements Function<ModelVersion, ResourceTransformationDescriptionBuilder> {

    @Override
    public ResourceTransformationDescriptionBuilder apply(ModelVersion version) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        // Transform infinispan-bean-management resource
        new BeanManagementResourceTransformer(builder).accept(version);

        // Transform infinispan-timer-management resource
        new TimerManagementResourceTransformer(builder).accept(version);

        return builder;
    }
}
