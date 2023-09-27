/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.function.Function;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

/**
 * @author Paul Ferraro
 */
public class DistributableWebResourceTransformer implements Function<ModelVersion, TransformationDescriptionBuilder> {

    @Override
    public ResourceTransformationDescriptionBuilder apply(ModelVersion version) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        new InfinispanSessionManagementResourceTransformer(builder).accept(version);
        new HotRodSessionManagementResourceTransformer(builder).accept(version);

        return builder;
    }
}
