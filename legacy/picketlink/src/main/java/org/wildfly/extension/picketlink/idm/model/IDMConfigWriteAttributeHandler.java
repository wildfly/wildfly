/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
