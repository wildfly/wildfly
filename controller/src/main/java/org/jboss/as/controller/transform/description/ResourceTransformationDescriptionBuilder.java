/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.transform.description;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.PathTransformation;
import org.jboss.as.controller.transform.ResourceTransformer;

/**
 * Common transformation description builder.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface ResourceTransformationDescriptionBuilder extends TransformationDescriptionBuilder {

    /**
     * Get a builder to define custom attribute transformation rules.
     *
     * @return the attribute transformation builder
     */
    AttributeTransformationDescriptionBuilder getAttributeBuilder();


    /**
     * Add an operation transformation entry for a specific operation.
     *
     * @param operationName the operation name
     * @return the operation transformation builder
     */
    OperationTransformationOverrideBuilder addOperationTransformationOverride(String operationName);

    /**
     * Register a raw operation transformer which is going to be used as is.
     *
     * @param operationName the operation name
     * @param operationTransformer the operation transformer
     * @return the builder for the current instance
     */
    ResourceTransformationDescriptionBuilder addRawOperationTransformationOverride(String operationName, OperationTransformer operationTransformer);

    /**
     * Set a custom resource transformer. This transformer is going to be called after all attribute transformations happened
     * and needs to take care of adding the currently transformed resource properly. If not specified, the resource will be
     * added according to other rules defined by this builder.
     *
     * @param resourceTransformer the resource transformer
     * @return the builder for the current resource
     */
    ResourceTransformationDescriptionBuilder setResourceTransformer(ResourceTransformer resourceTransformer);

    /**
     * Add a child resource.
     *
     * @param pathElement the path element
     * @return the builder for the child resource
     */
    ResourceTransformationDescriptionBuilder addChildResource(PathElement pathElement);

    /**
     * Add a child resource, where all operations will get redirected to the new element address.
     *
     * @param oldAddress the old path element
     * @param newAddress the new path element
     * @return the builder for the child resource
     */
    ResourceTransformationDescriptionBuilder addChildRedirection(PathElement oldAddress, PathElement newAddress);

    /**
     * Add a child resource, where all operation will get redirected to a different address defined by
     * the path transformation.
     *
     * @param pathElement the path element of the child
     * @param pathTransformation the path transformation
     * @return the builder for the child resource
     */
    ResourceTransformationDescriptionBuilder addChildRedirection(PathElement pathElement, PathTransformation pathTransformation);

    /**
     * Add a child resource.
     *
     * @param definition the resource definition
     * @return the builder for the child resource
     */
    ResourceTransformationDescriptionBuilder addChildResource(ResourceDefinition definition);

    /**
     * Recursively discards all child resources and its operations.
     *
     * @param pathElement the path element
     * @return the builder for the child resource
     */
    DiscardTransformationDescriptionBuilder discardChildResource(PathElement pathElement);

}
