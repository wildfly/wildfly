/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.dynamicresource;

import org.jboss.as.connector.subsystems.resourceadapters.Constants;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.workmanager.WorkManager;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.STATISTICS_NAME;

/**
 * Clear stats from passed plugins
 *
 * @author Stefano Maestri
 */
public class ClearWorkManagerStatisticsHandler implements OperationStepHandler {

    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(Constants.CLEAR_STATISTICS, ResourceAdaptersExtension.getResourceDescriptionResolver(STATISTICS_NAME))
            .build();

    private final WorkManager wm;
    public ClearWorkManagerStatisticsHandler(final WorkManager wm) {
        this.wm = wm;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        if (context.isNormalServer()) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    wm.getStatistics().clear();
                    context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }
}

