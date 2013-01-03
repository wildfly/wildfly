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
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * {@code OperationTransformer} transforming the operation address only.
 *
 * @author Emanuel Muckenhuber
 */
public class AliasOperationTransformer implements OperationTransformer {

    public interface AddressTransformer {

        /**
         * Transform an address.
         *
         * @param address the address to transform
         * @return the transformed address
         */
        PathAddress transformAddress(PathAddress address);

    }

    private final AddressTransformer transformer;
    protected AliasOperationTransformer(AddressTransformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode original) throws OperationFailedException{
        final ModelNode operation = original.clone();
        final PathAddress transformedAddress = transformer.transformAddress(address);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(transformedAddress.toModelNode());

        // Hand-off to a local operation transformer at the right address
        final String operationName = operation.get(ModelDescriptionConstants.OP).asString();
        final OperationTransformer aliasTransformer = context.getTarget().resolveTransformer(transformedAddress, operationName);
        return aliasTransformer.transformOperation(context, transformedAddress, operation);
        // return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
    }

    /**
     * Replace the last element of an address with a static path element.
     *
     * @param element the path element
     * @return the operation address transformer
     */
    public static AliasOperationTransformer replaceLastElement(final PathElement element) {
        return create(new AddressTransformer() {
            @Override
            public PathAddress transformAddress(final PathAddress original) {
                final PathAddress address = original.subAddress(0, original.size() -1);
                return address.append(element);
            }
        });
    }

    public static AliasOperationTransformer create(final AddressTransformer transformer) {
        return new AliasOperationTransformer(transformer);
    }

}
