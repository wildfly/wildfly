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

package org.jboss.as.clustering.controller.transform;

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;

/**
 * @author Radoslav Husar
 * @version Jun 2015
 */
public class SimplePathOperationTransformer implements OperationTransformer {

    private final PathAddressTransformer addressTransformer;

    public SimplePathOperationTransformer(PathAddressTransformer addressTransformer) {
        this.addressTransformer = addressTransformer;
    }

    @Override
    public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) {
        ModelNode legacyOperation = operation.clone();
        Operations.setPathAddress(legacyOperation, this.addressTransformer.transform(address));

        InitialAttributeValueOperationContextAttachment attachment = context.getAttachment(InitialAttributeValueOperationContextAttachment.INITIAL_VALUES_ATTACHMENT);
        if (attachment != null) {
            ModelNode value = attachment.getInitialValue(address, Operations.getAttributeName(operation));
            if (value != null) {
                attachment.putIfAbsentInitialValue(this.addressTransformer.transform(address), Operations.getAttributeName(operation), value);
            }
        }

        return new TransformedOperation(legacyOperation, OperationResultTransformer.ORIGINAL_RESULT);
    }
}
