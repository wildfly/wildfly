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

package org.wildfly.extension.picketlink.idm.model;

import java.util.function.Function;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.picketlink.common.model.validator.ModelValidationStepHandler;

/**
 * @author Pedro Silva
 */
public class IDMConfigWriteAttributeHandler extends ModelOnlyWriteAttributeHandler {

    private final ModelValidationStepHandler[] modelValidators;
    private final Function<PathAddress, PathAddress> partitionAddressProvider;

    IDMConfigWriteAttributeHandler(final ModelValidationStepHandler[] modelValidators,
            final Function<PathAddress, PathAddress> partitionAddressProvider, final AttributeDefinition... attributes) {
        super(attributes);
        this.modelValidators = modelValidators;
        this.partitionAddressProvider = partitionAddressProvider;
    }
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        if (modelValidators != null) {

            for (ModelValidationStepHandler validator : modelValidators) {
                context.addStep(validator, OperationContext.Stage.MODEL);
            }
        }

        if (partitionAddressProvider != null) {
            context.addStep((context1, operation1) -> PartitionManagerResourceDefinition.validateModel(context, partitionAddressProvider.apply(context1.getCurrentAddress())),
                    OperationContext.Stage.MODEL);
        }

        super.execute(context, operation);
    }
}
