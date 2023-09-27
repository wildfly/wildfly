/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.common.pool;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.spi.statistics.StatisticsPlugin;


public class PoolStatisticsRuntimeAttributeWriteHandler implements OperationStepHandler {

    private final StatisticsPlugin stats;

    private final ParametersValidator nameValidator = new ParametersValidator();


    public PoolStatisticsRuntimeAttributeWriteHandler(final StatisticsPlugin stats) {
        this.stats = stats;

    }


    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        nameValidator.validate(operation);
        final String attributeName = operation.require(NAME).asString();
        // Don't require VALUE. Let the validator decide if it's bothered by an undefined value

        ModelNode newValue = operation.hasDefined(VALUE) ? operation.get(VALUE) : new ModelNode();

        final ModelNode resolvedValue = newValue.resolve();

        switch (attributeName) {
            case ModelDescriptionConstants.STATISTICS_ENABLED: {
                stats.setEnabled(resolvedValue.asBoolean());
                break;
            }

        }
    }

}
