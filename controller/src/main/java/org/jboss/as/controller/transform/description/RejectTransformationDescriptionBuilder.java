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

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.PathAddressTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RejectTransformationDescriptionBuilder extends AbstractTransformationDescriptionBuilder implements TransformationDescriptionBuilder {

    protected RejectTransformationDescriptionBuilder(PathElement pathElement) {
        super(pathElement, PathAddressTransformer.DEFAULT, REJECT_RESOURCE, REJECT_OPERATION);
    }

    @Override
    public TransformationDescription build() {
        return new RejectDefinition(pathElement, REJECT_RESOURCE, REJECT_OPERATION);
    }

    private static OperationTransformer REJECT_OPERATION = new OperationTransformer() {

        @Override
        public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation) throws OperationFailedException {
            if (context.getTarget().isIgnoredResourceListAvailableAtRegistration()) {
                throw new OperationFailedException(ControllerMessages.MESSAGES.rejectResourceOperationTransformation(address, operation));
            }
            return new TransformedOperation(null, new OperationRejectionPolicy() {

                @Override
                public boolean rejectOperation(ModelNode preparedResult) {
                    return true;
                }

                @Override
                public String getFailureDescription() {
                    return ControllerMessages.MESSAGES.rejectResourceOperationTransformation(address, operation);
                }
            }, OperationResultTransformer.ORIGINAL_RESULT);
        }
    };

    private static ResourceTransformer REJECT_RESOURCE = new ResourceTransformer() {
        @Override
        public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) {
            context.getLogger().logRejectedResourceWarning(address, null);
        }
    };
}

