/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;

/**
 * Describes the common properties of a remove operation handler.
 * @author Paul Ferraro
 */
public interface AddStepHandlerDescriptor extends WriteAttributeStepHandlerDescriptor, RemoveStepHandlerDescriptor {

    /**
     * Custom attributes of the add operation, processed using a specific write-attribute handler.
     * @return a map of attributes and their write-attribute handler
     */
    Map<AttributeDefinition, OperationStepHandler> getCustomAttributes();

    /**
     * Extra parameters (not specified by {@link #getAttributes()}) for the add operation.
     * @return a collection of attributes
     */
    Collection<AttributeDefinition> getExtraParameters();

    /**
     * Returns the required child resources for this resource description.
     * @return a collection of resource paths
     */
    Set<PathElement> getRequiredChildren();

    /**
     * Returns the required singleton child resources for this resource description.
     * This means only one child resource should exist for the given child type.
     * @return a collection of resource paths
     */
    Set<PathElement> getRequiredSingletonChildren();

    /**
     * Returns a mapping of attribute translations
     * @return an attribute translation mapping
     */
    Map<AttributeDefinition, AttributeTranslation> getAttributeTranslations();

    /**
     * Returns a transformer for the add operation handler.
     * This is typically used to adapt legacy operations to conform to the current version of the model.
     * @return an operation handler transformer.
     */
    UnaryOperator<OperationStepHandler> getAddOperationTransformation();

    /**
     * Returns a transformation for a newly created resource.
     * @return a resource transformation
     */
    UnaryOperator<Resource> getResourceTransformation();
}
