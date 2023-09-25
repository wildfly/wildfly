/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.idm.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.picketlink.common.model.validator.ModelValidationStepHandler;

/**
 * @author Pedro Silva
 */
public class DefaultAddStepHandler extends ModelOnlyAddStepHandler {

    private final List<ModelValidationStepHandler> modelValidators = new ArrayList<>();
    private final Function<PathAddress, PathAddress> partitionAddressProvider;

    DefaultAddStepHandler(ModelValidationStepHandler[] modelValidators,
                          final Function<PathAddress, PathAddress> partitionAddressProvider,
                          final AttributeDefinition... attributes) {
        super(attributes);
        this.partitionAddressProvider = partitionAddressProvider;
        configureModelValidators(modelValidators);
    }

    private void configureModelValidators(ModelValidationStepHandler[] modelValidators) {

        if (modelValidators != null) {
            this.modelValidators.addAll(Arrays.asList(modelValidators));
        }

        if (partitionAddressProvider != null) {
            this.modelValidators.add(new ModelValidationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    PartitionManagerResourceDefinition.validateModel(context, partitionAddressProvider.apply(context.getCurrentAddress()));
                }
            });
        }
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        performValidation(context);
        super.execute(context, operation);
    }

    protected void performValidation(OperationContext context) {
        for (ModelValidationStepHandler validatonStepHandler : this.modelValidators) {
            context.addStep(validatonStepHandler, OperationContext.Stage.MODEL);
        }
    }
}
