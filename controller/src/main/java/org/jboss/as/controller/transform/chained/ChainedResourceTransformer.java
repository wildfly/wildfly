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
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

/**
 * An experimental resource transformer allowing you to combine several transformers.
 * TODO Add the ability to remove child resources - ping me if needed :-)
 *
 * @deprecated Use {@link TransformationDescriptionBuilder} instead
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@Deprecated
public class ChainedResourceTransformer implements ResourceTransformer {

    private final ChainedResourceTransformerEntry[] entries;

    public ChainedResourceTransformer(ChainedResourceTransformerEntry...entries) {
        this.entries = entries;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource)
            throws OperationFailedException {
        if (resource.isProxy() || resource.isRuntime()) {
            return;
        }

        @SuppressWarnings("deprecation")
        ChainedResourceTransformationContext wrappedContext = new ChainedResourceTransformationContext(context);
        for (ChainedResourceTransformerEntry entry : entries) {
            entry.transformResource(wrappedContext, address, resource);
        }

        final ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
        childContext.processChildren(resource);
    }
}
