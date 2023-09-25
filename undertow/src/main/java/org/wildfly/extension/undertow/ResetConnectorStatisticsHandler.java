/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import io.undertow.server.ConnectorStatistics;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;


/**
 * Defines and handles resetting connector statistics
 *
 * @author Stuart Douglas
 */
public class ResetConnectorStatisticsHandler implements OperationStepHandler {
    public static final String OPERATION_NAME = "reset-statistics";

    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, UndertowExtension.getResolver("listener"))
            .setRuntimeOnly()
            .build();

    public static final ResetConnectorStatisticsHandler INSTANCE = new ResetConnectorStatisticsHandler();

    private ResetConnectorStatisticsHandler() {

    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        ListenerService service = ListenerResourceDefinition.getListenerService(context);
        if (service != null) {
            ConnectorStatistics stats = service.getOpenListener().getConnectorStatistics();
            if (stats != null) {
                stats.reset();
            }
        }
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

}
