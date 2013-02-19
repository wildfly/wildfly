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
package org.jboss.as.controller.transform;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;

/**
 * {@link ResourceTransformer} that takes the value in the last element of the given address
 * and stores it in a model attribute named {@code name}.
 * <p>
 * This transformer can be used to preserve compatibility when {@link org.jboss.as.controller.ReadResourceNameOperationStepHandler} is
 * used to replace storage of a resource name in the model.
 * </p>
 *
 * @see org.jboss.as.controller.ReadResourceNameOperationStepHandler
 */
@SuppressWarnings("deprecation")
public class AddNameFromAddressResourceTransformer implements ResourceTransformer {
    public static final AddNameFromAddressResourceTransformer INSTANCE = new AddNameFromAddressResourceTransformer();

    private AddNameFromAddressResourceTransformer() {
    }

    @Override
    public void transformResource(final ResourceTransformationContext context, final PathAddress address, final Resource resource) throws OperationFailedException {

        transformResourceInternal(address, resource);
        ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
        childContext.processChildren(resource);
    }

    private void transformResourceInternal(final PathAddress address, final Resource resource) throws OperationFailedException {

        final PathElement element = address.getLastElement();
        resource.getModel().get(NAME).set(element.getValue());
    }
}
