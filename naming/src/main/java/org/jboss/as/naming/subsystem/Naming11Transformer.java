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
package org.jboss.as.naming.subsystem;

import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING_TYPE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.OBJECT_FACTORY;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.OBJECT_FACTORY_ENV;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.SIMPLE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.TYPE;

import java.net.URL;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.CombinedTransformer;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.naming.NamingMessages;
import org.jboss.dmr.ModelNode;

/**
 * A transform used to reject new unsupported operations, on legacy models.
 *
 * @author Eduardo Martins
 */
public class Naming11Transformer implements CombinedTransformer {
    @Override
    public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
        //todo add warn msg
        resource.getModel().remove(OBJECT_FACTORY_ENV);
    }

    @Override
    public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation)
            throws OperationFailedException {
        // check the operation
        final ModelNode resolvedModel = operation.clone();
        if (resolvedModel.hasDefined(BINDING_TYPE)) {
            final String type = resolvedModel.get(BINDING_TYPE).asString();
            if (type.equals(SIMPLE) && resolvedModel.hasDefined(TYPE)) {
                if (URL.class.getName().equals(resolvedModel.get(TYPE).asString())) {
                    // simple binding with type URL, not supported on 1.1.0
                    return new TransformedOperation(operation, rejectOperation(NamingMessages.MESSAGES
                            .failedToTransformSimpleURLNameBindingAddOperation("1.1.0")), OperationResultTransformer.ORIGINAL_RESULT);
                }
            } else if (type.equals(OBJECT_FACTORY) && resolvedModel.hasDefined(OBJECT_FACTORY_ENV)) {
                // object factory bind with environment, not supported on 1.1.0
                operation.remove(OBJECT_FACTORY_ENV);
                return new TransformedOperation(
                        operation,
                        rejectOperation(NamingMessages.MESSAGES.failedToTransformObjectFactoryWithEnvironmentNameBindingAddOperation("1.1.0")),
                        OperationResultTransformer.ORIGINAL_RESULT);
            }
        }
        // all good, return untransformed
        return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
    }

    /**
     * Reject an operation if applied successfully.
     *
     */
    private OperationRejectionPolicy rejectOperation(final String failureMessage) {
        return new OperationRejectionPolicy() {
            @Override
            public boolean rejectOperation(ModelNode preparedResult) {
                return true;
            }

            @Override
            public String getFailureDescription() {
                return failureMessage;
            }
        };
    }

}
