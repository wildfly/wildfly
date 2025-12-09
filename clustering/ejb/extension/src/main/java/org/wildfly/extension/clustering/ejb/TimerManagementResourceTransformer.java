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
 * Transformer for timer management resources.
 * @author Radoslav Husar
 */
public class TimerManagementResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder builder;

    TimerManagementResourceTransformer(ResourceTransformationDescriptionBuilder builder) {
        this.builder = builder;
    }

    @Override
    public void accept(ModelVersion version) {
        if (DistributableEjbSubsystemModel.VERSION_2_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, InfinispanTimerManagementResourceDefinitionRegistrar.IDLE_THRESHOLD)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, InfinispanTimerManagementResourceDefinitionRegistrar.IDLE_THRESHOLD)
                    .end();
        }
    }
}
