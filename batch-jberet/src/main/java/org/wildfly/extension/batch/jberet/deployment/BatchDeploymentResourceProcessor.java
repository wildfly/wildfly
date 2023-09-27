/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.deployment;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentResourceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.wildfly.extension.batch.jberet._private.BatchLogger;

/**
 * Process deployments to add runtime deployment resources for the batch-jberet subsystem
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchDeploymentResourceProcessor implements DeploymentUnitProcessor {
    private final String subsystemName;

    public BatchDeploymentResourceProcessor(final String subsystemName) {
        this.subsystemName = subsystemName;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.hasAttachment(Attachments.MODULE) && !DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit) && deploymentUnit.hasAttachment(Attachments.DEPLOYMENT_ROOT)) {
            BatchLogger.LOGGER.tracef("Processing deployment '%s' for the batch deployment resources.", deploymentUnit.getName());
            final DeploymentResourceSupport deploymentResourceSupport = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
            // Add the job operator service used interact with a deployments batch job
            final WildFlyJobOperator jobOperator = deploymentUnit.getAttachment(BatchAttachments.JOB_OPERATOR);

            // Process each job XML file
            for (String jobName : jobOperator.getAllJobNames()) {
                try {
                    // Add the job information to the service
                    BatchLogger.LOGGER.debugf("Added job %s to allowed jobs for deployment %s", jobName, deploymentUnit.getName());
                    // Register the a resource for each job found
                    final PathAddress jobAddress = PathAddress.pathAddress(BatchJobResourceDefinition.JOB, jobName);
                    if (!deploymentResourceSupport.hasDeploymentSubModel(subsystemName, jobAddress)) {
                        deploymentResourceSupport.registerDeploymentSubResource(subsystemName,
                                jobAddress, new BatchJobExecutionResource(jobOperator, jobName));
                    }
                } catch (Exception e) {
                    // The deployment shouldn't fail in this case, just the specific resource registration should be skipped
                    // Log a debug message so the error is not lost
                    BatchLogger.LOGGER.debugf(e, "Batch jobs as an error occurred will not be registered for runtime views on the deployment (%s).", jobName, deploymentUnit.getName());
                }
            }
        }
    }
}
