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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;

import java.util.Collection;

/**
 * @author Emanuel Muckenhuber
 */
public abstract class ResourceTransformationDescriptionBuilder extends TransformationDescriptionBuilder {

    /**
     * Reject expressions as attribute values.
     *
     * @param attributeNames the attributes names
     * @return the builder for the current resource
     */
    public abstract ResourceTransformationDescriptionBuilder rejectExpressions(Collection<String> attributeNames);

    /**
     * Reject expressions as attribute values.
     *
     * @param attributeNames the attribute names
     * @return the builder for the current resource
     */
    public abstract ResourceTransformationDescriptionBuilder rejectExpressions(String... attributeNames);

    /**
     * Reject expressions as attributes values.
     *
     * @param defintions the attribute definitions
     * @return the builder for the current resource
     */
    public abstract ResourceTransformationDescriptionBuilder rejectExpressions(AttributeDefinition... defintions);

    /**
     * Add a custom transformation step. This will be called for model as well as operation transformation.
     *
     * @param transformer the model transformer
     */
    public abstract ResourceTransformationDescriptionBuilder addCustomTransformation(ModelTransformer transformer);

    /**
     * Add a child resource.
     *
     * @param pathElement the path element
     * @return the builder for the child resource
     */
    public abstract ResourceTransformationDescriptionBuilder addChildResource(PathElement pathElement);

    /**
     * Add a child resource.
     *
     * @param definition the resource definition
     * @return the builder for the child resource
     */
    public abstract ResourceTransformationDescriptionBuilder addChildResource(ResourceDefinition definition);

    /**
     * Recursively discards all child resources and its operations.
     *
     * @param pathElement the path element
     * @return the builder for the child resource
     */
    public abstract TransformationDescriptionBuilder discardChildResource(PathElement pathElement);

}
