/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

