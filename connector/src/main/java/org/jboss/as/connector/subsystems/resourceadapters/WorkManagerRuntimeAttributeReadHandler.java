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

package org.jboss.as.connector.subsystems.resourceadapters;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.workmanager.DistributedWorkManager;
import org.jboss.jca.core.api.workmanager.WorkManager;
import org.jboss.jca.core.api.workmanager.WorkManagerStatistics;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;

public class WorkManagerRuntimeAttributeReadHandler implements OperationStepHandler {

    private final WorkManagerStatistics wmStat;
    private final WorkManager wm;
    private final boolean distributed;

    public WorkManagerRuntimeAttributeReadHandler(WorkManager wm, final WorkManagerStatistics wmStat, boolean distributed) {
        this.wm = wm;
        this.wmStat = wmStat;
        this.distributed = distributed;
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
                            case Constants.WORK_ACTIVE_NAME: {
                                result.set(wmStat.getWorkActive());
                                break;
                            }
                            case Constants.WORK_FAILED_NAME: {
                                result.set(wmStat.getWorkFailed());
                                break;
                            }
                            case Constants.WORK_SUCEESSFUL_NAME: {
                                result.set(wmStat.getWorkSuccessful());
                                break;
                            }
                            case Constants.DO_WORK_ACCEPTED_NAME: {
                                result.set(wmStat.getDoWorkAccepted());
                                break;
                            }
                            case Constants.DO_WORK_REJECTED_NAME: {
                                result.set(wmStat.getDoWorkRejected());
                                break;
                            }
                            case Constants.SCHEDULED_WORK_ACCEPTED_NAME: {
                                result.set(wmStat.getScheduleWorkAccepted());
                                break;
                            }
                            case Constants.SCHEDULED_WORK_REJECTED_NAME: {
                                result.set(wmStat.getScheduleWorkRejected());
                                break;
                            }
                            case Constants.START_WORK_ACCEPTED_NAME: {
                                result.set(wmStat.getStartWorkAccepted());
                                break;
                            }
                            case Constants.START_WORK_REJECTED_NAME: {
                                result.set(wmStat.getStartWorkRejected());
                                break;
                            }
                            case ModelDescriptionConstants.STATISTICS_ENABLED: {
                                if (distributed) {
                                    result.set(((DistributedWorkManager) wm).isDistributedStatisticsEnabled());
                                } else {
                                    result.set(wm.isStatisticsEnabled());
                                }
                                break;
                            }
                            case Constants.WORKMANAGER_STATISTICS_ENABLED_NAME: {
                                result.set(wm.isStatisticsEnabled());
                                break;
                            }
                            case Constants.DISTRIBUTED_STATISTICS_ENABLED_NAME: {
                                result.set(((DistributedWorkManager) wm).isDistributedStatisticsEnabled());
                                break;
                            }
                            case Constants.DOWORK_DISTRIBUTION_ENABLED_NAME: {
                                result.set(((DistributedWorkManager) wm).isDoWorkDistributionEnabled());
                                break;
                            }
                            case Constants.STARTWORK_DISTRIBUTION_ENABLED_NAME: {
                                result.set(((DistributedWorkManager) wm).isStartWorkDistributionEnabled());
                                break;
                            }
                            case Constants.SCHEDULEWORK_DISTRIBUTION_ENABLED_NAME: {
                                result.set(((DistributedWorkManager) wm).isScheduleWorkDistributionEnabled());
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
