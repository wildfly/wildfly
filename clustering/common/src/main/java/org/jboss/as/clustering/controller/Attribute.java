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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Interface to be implemented by attribute enumerations.
 * @author Paul Ferraro
 */
public interface Attribute extends Definable<AttributeDefinition> {

    /**
     * Returns the name of this attribute.
     * @return the attribute name
     */
    default String getName() {
        return this.getDefinition().getName();
    }

    /**
     * Resolves the value of this attribute from the specified model applying any default value.
     * @param resolver an expression resolver
     * @param model the resource model
     * @return the resolved value
     * @throws OperationFailedException if the value was not valid
     */
    default ModelNode resolveModelAttribute(ExpressionResolver resolver, ModelNode model) throws OperationFailedException {
        return this.getDefinition().resolveModelAttribute(resolver, model);
    }
}
