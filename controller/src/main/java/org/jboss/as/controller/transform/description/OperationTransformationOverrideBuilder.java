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

import org.jboss.as.controller.transform.OperationTransformer;

/**
 * Transformation builder interface for overriding a given operation.
 *
 * @author Emanuel Muckenhuber
 */
public interface OperationTransformationOverrideBuilder extends AttributeTransformationDescriptionBuilder {

    /**
     * Give the operation a new name
     *
     * @param newName the new name of the operation
     * @return this operation transformer builder
     */
    OperationTransformationOverrideBuilder rename(String newName);

    /**
     * Set an optional operation transformer, which is called after all attribute rules were executed.
     *
     * @param operationTransformer the operation transformer
     * @return this operation transformer builder
     */
    OperationTransformationOverrideBuilder setCustomOperationTransformer(OperationTransformer operationTransformer);

    /**
     * Inherit all existing attribute rules from the resource for this operation transformer.
     *
     * @return this operation transformer builder
     */
    OperationTransformationOverrideBuilder inheritResourceAttributeDefinitions();

}
