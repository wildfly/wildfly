/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.AttributeDefinition;
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

    public static void registerTransformersFrom700to600(ResourceTransformationDescriptionBuilder builder, boolean rejectVirtual) {
        if (rejectVirtual) {
            addVirtualNonDefaultReject(builder, EESubsystemModel.MANAGED_EXECUTOR_SERVICE, ManagedExecutorServiceResourceDefinition.VIRTUAL_AD);
            addVirtualNonDefaultReject(builder, EESubsystemModel.MANAGED_SCHEDULED_EXECUTOR_SERVICE, ManagedScheduledExecutorServiceResourceDefinition.VIRTUAL_AD);
            addVirtualNonDefaultReject(builder, EESubsystemModel.MANAGED_THREAD_FACTORY, ManagedThreadFactoryResourceDefinition.VIRTUAL_AD);
        } else {
            addVirtualDiscard(builder, EESubsystemModel.MANAGED_EXECUTOR_SERVICE, ManagedExecutorServiceResourceDefinition.VIRTUAL_AD);
            addVirtualDiscard(builder, EESubsystemModel.MANAGED_SCHEDULED_EXECUTOR_SERVICE, ManagedScheduledExecutorServiceResourceDefinition.VIRTUAL_AD);
            addVirtualDiscard(builder, EESubsystemModel.MANAGED_THREAD_FACTORY, ManagedThreadFactoryResourceDefinition.VIRTUAL_AD);
        }
    }

    private static void addVirtualNonDefaultReject(ResourceTransformationDescriptionBuilder builder, String resourceType,
                                              AttributeDefinition virtualAttribute) {
        builder.addChildResource(PathElement.pathElement(resourceType))
                .getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, virtualAttribute)
                .addRejectCheck(RejectAttributeChecker.DEFINED, virtualAttribute);
    }

    private static void addVirtualDiscard(ResourceTransformationDescriptionBuilder builder, String resourceType,
                                              AttributeDefinition virtualAttribute) {
        builder.addChildResource(PathElement.pathElement(resourceType))
                .getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.ALWAYS, virtualAttribute);
    }
}
