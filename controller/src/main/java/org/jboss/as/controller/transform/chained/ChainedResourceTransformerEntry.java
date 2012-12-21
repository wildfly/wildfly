/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.transform.chained;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;

/**
 * Provides chained transformation of resources.
 * 
 * @deprecated Experimental and likely to change
 * @author Kabir Khan
 */
@Deprecated
public interface ChainedResourceTransformerEntry extends ResourceTransformer {

    /**
     * Same as {@link ResourceTransformer#transformResource(ResourceTransformationContext, PathAddress, Resource)} with the exception
     * that you cannot call {@link ResourceTransformationContext#addTransformedResource(PathAddress, Resource)}, {@link ResourceTransformationContext#addTransformedResourceFromRoot(PathAddress, Resource)},
     * {@link ResourceTransformationContext#addTransformedRecursiveResource(PathAddress, Resource)}, {@link ResourceTransformationContext#processChild(PathElement, Resource)} or
     * {@link ResourceTransformationContext#processChildren(Resource)} on the passed in {@code context}.
     */
    void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException;
}