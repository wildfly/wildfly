/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.dynamicresource;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONNECTIONDEFINITIONS_NAME;

import java.util.Arrays;
import java.util.List;

import org.jboss.as.connector.subsystems.resourceadapters.Constants;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.spi.statistics.StatisticsPlugin;

/**
 * Clear stats from passed plugins
 *
 * @author Stefano Maestri
 */
public class ClearStatisticsHandler implements OperationStepHandler {

    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(Constants.CLEAR_STATISTICS, ResourceAdaptersExtension.getResourceDescriptionResolver(CONNECTIONDEFINITIONS_NAME))
            .build();

    private final List<StatisticsPlugin> stats;

    public ClearStatisticsHandler(StatisticsPlugin... stats) {
        this.stats = Arrays.asList(stats);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        if (context.isNormalServer()) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    for (StatisticsPlugin statsPlugin : stats) {
                        statsPlugin.clear();
                    }
                    context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }
}

