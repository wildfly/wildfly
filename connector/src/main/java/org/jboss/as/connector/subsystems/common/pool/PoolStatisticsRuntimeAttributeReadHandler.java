/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.subsystems.common.pool;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.spi.statistics.StatisticsPlugin;

public class PoolStatisticsRuntimeAttributeReadHandler implements OperationStepHandler {

    private final StatisticsPlugin stats;

    public PoolStatisticsRuntimeAttributeReadHandler(StatisticsPlugin stats) {
        this.stats = stats;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (context.isNormalServer()) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final String attributeName = operation.require(NAME).asString();
                    try {
                        final ModelNode result = context.getResult();

                        switch (attributeName) {

                            case ModelDescriptionConstants.STATISTICS_ENABLED: {
                                result.set(stats.isEnabled());
                                break;
                            }
                        }

                    } catch (Exception e) {
                        throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.failedToGetMetrics(e.getLocalizedMessage()));
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }

}
