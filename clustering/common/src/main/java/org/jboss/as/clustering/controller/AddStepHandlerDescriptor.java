/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.controller;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;

/**
 * Describes the common properties of a remove operation handler.
 * @author Paul Ferraro
 */
public interface AddStepHandlerDescriptor extends WriteAttributeStepHandlerDescriptor, RemoveStepHandlerDescriptor {

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
}
