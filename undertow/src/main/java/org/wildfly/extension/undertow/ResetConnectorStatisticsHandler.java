/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import io.undertow.server.ConnectorStatistics;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;


/**
 * Defines and handles reseting connector statistics
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
