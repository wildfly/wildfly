/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transformer for bean management resources.
 * @author Radoslav Husar
 */
public class BeanManagementResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder parent;

    BeanManagementResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.parent = parent;
    }

    @Override
    public void accept(ModelVersion version) {
        ResourceTransformationDescriptionBuilder builder = this.parent.addChildResource(BeanManagementResourceRegistration.INFINISPAN.getPathElement());

        if (DistributableEjbSubsystemModel.VERSION_2_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, BeanManagementResourceDefinitionRegistrar.IDLE_THRESHOLD)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, BeanManagementResourceDefinitionRegistrar.IDLE_THRESHOLD)
                    .end();
        }
    }
}
