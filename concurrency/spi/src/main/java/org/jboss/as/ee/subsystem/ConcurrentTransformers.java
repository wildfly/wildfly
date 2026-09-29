/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Provides methods to implement calls to
 * {@link org.jboss.as.ee.concurrent.ConcurrencyImplementation#registerTransformers(ResourceTransformationDescriptionBuilder, ModelVersion)}
 */
public final class ConcurrentTransformers {

    public static void registerTransformersFrom700to600(ResourceTransformationDescriptionBuilder builder) {
        builder.addChildResource(PathElement.pathElement(EESubsystemModel.MANAGED_EXECUTOR_SERVICE))
                .getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, ManagedExecutorServiceResourceDefinition.VIRTUAL_AD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ManagedExecutorServiceResourceDefinition.VIRTUAL_AD);
        builder.addChildResource(PathElement.pathElement(EESubsystemModel.MANAGED_SCHEDULED_EXECUTOR_SERVICE))
                .getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, ManagedScheduledExecutorServiceResourceDefinition.VIRTUAL_AD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ManagedScheduledExecutorServiceResourceDefinition.VIRTUAL_AD);
        builder.addChildResource(PathElement.pathElement(EESubsystemModel.MANAGED_THREAD_FACTORY))
                .getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, ManagedThreadFactoryResourceDefinition.VIRTUAL_AD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ManagedThreadFactoryResourceDefinition.VIRTUAL_AD);
    }
}
