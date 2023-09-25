/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;

/**
 * Describes the common properties of a remove operation handler.
 * @author Paul Ferraro
 */
@FunctionalInterface
public interface RemoveStepHandlerDescriptor extends OperationStepHandlerDescriptor {
    /**
     * The description resolver for the operation.
     * @return a description resolver
     */
    ResourceDescriptionResolver getDescriptionResolver();

    /**
     * Returns a collection of handlers that register runtime resources
     * Runtime resource registrations are executed in a separate MODEL stage step.
     * @return a collection of operation step handlers
     */
    default Collection<RuntimeResourceRegistration> getRuntimeResourceRegistrations() {
        return Collections.emptyList();
    }

    /**
     * Returns a mapping of capability references to an ancestor resource.
     * @return a tuple of capability references and requirement resolvers.
     */
    default Set<CapabilityReferenceRecorder> getResourceCapabilityReferences() {
        return Collections.emptySet();
    }

    /**
     * Returns a transformer to be applied to all operations that operate on an existing resource.
     * This is typically used to adapt legacy operations to conform to the current version of the model.
     * @return an operation handler transformer.
     */
    default UnaryOperator<OperationStepHandler> getOperationTransformation() {
        return UnaryOperator.identity();
    }
}
