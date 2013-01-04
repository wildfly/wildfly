/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.transform;

import org.jboss.as.controller.PathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

/**
 * Registration for subsystem specific operation transformers.
 *
 * @author Emanuel Muckenhuber
 */
public interface TransformersSubRegistration {

    String[] COMMON_OPERATIONS = { ADD, REMOVE };

    /**
     * Register a sub resource.
     *
     * @param element the path element
     * @return the sub registration
     */
    TransformersSubRegistration registerSubResource(PathElement element);

    /**
     * Register a sub resource. If discardByDefault is set to {@code true}, both operations and resource transformations
     * are going to discard operations addressed to this resource.
     *
     * @param element the path element
     * @param discardByDefault don't forward operations by default
     * @return the sub registration
     */
    TransformersSubRegistration registerSubResource(PathElement element, boolean discardByDefault);

    /**
     * register a sub resource.
     *
     * @param element the path element
     * @param resourceTransformer the resource transformer
     * @return the transformers sub registration
     */
    TransformersSubRegistration registerSubResource(PathElement element, ResourceTransformer resourceTransformer);

    /**
     * Register a sub resource.
     *
     * @param element the path element
     * @param operationTransformer the default operation transformer
     * @return the sub registration
     */
    TransformersSubRegistration registerSubResource(PathElement element, OperationTransformer operationTransformer);

    /**
     * Register a sub resource.
     *
     * @param element the path element
     * @param resourceTransformer the resource transformer
     * @param operationTransformer the default operation transformer
     * @return the transformers sub registration
     */
    TransformersSubRegistration registerSubResource(PathElement element, ResourceTransformer resourceTransformer, OperationTransformer operationTransformer);

    /**
     * Register a sub resource.
     *
     * @param element the path element
     * @param transformer the resource and operation transformer
     * @return the transformers sub registration
     */
    TransformersSubRegistration registerSubResource(PathElement element, CombinedTransformer transformer);

    /**
     * Don't forward and just discard the operation.
     *
     * @param operationNames the operation names
     */
    void discardOperations(String... operationNames);

    /**
     * Register an operation transformer.
     *
     * @param operationName the operation name
     * @param transformer the operation transformer
     */
    void registerOperationTransformer(String operationName, OperationTransformer transformer);

}
