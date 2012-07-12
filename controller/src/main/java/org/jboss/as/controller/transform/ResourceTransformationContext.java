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

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;

/**
 * The resource transformation context.
 *
* @author Emanuel Muckenhuber
*/
public interface ResourceTransformationContext extends TransformationContext {

    /**
     * Add a resource.
     *
     * @param relativeAddress the relative address
     * @param resource the resource model to add
     * @return the resource transformation context
     */
    ResourceTransformationContext addTransformedResource(PathAddress relativeAddress, Resource resource);

    /**
     * Add a resource from the root of the model.
     *
     * @param absoluteAddress the absolute address
     * @param resource the resource model to add
     * @return the resource transformation context
     */
    ResourceTransformationContext addTransformedResourceFromRoot(PathAddress absoluteAddress, Resource resource);

    /**
     * Add a resource recursively including it's children.
     *
     * @param relativeAddress the relative address
     * @param resource the resource to add
     */
    void addTransformedRecursiveResource(PathAddress relativeAddress, Resource resource);

    /**
     * Resolve the resource transformer for a given address.
     *
     * @param address the absolute address
     * @return the resource transformer
     */
    ResourceTransformer resolveTransformer(PathAddress address);

    /**
     * Process all children of a given resource.
     *
     * @param resource the resource
     * @throws OperationFailedException
     */
    void processChildren(Resource resource) throws OperationFailedException;

    /**
     * Process a child.
     *
     * @param element the path element
     * @param child the child
     * @throws  OperationFailedException
     */
    void processChild(PathElement element, Resource child) throws OperationFailedException;

    /**
     * Get the transformed root.
     *
     * @return the transformed root
     */
    Resource getTransformedRoot();

}
