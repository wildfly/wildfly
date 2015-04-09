/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.batch.deployment;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentResourceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.wildfly.extension.batch.BatchServiceNames;
import org.wildfly.extension.batch.BatchSubsystemDefinition;
import org.wildfly.extension.batch._private.BatchLogger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchDeploymentResourceProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.hasAttachment(Attachments.MODULE) && !DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit) && deploymentUnit.hasAttachment(Attachments.DEPLOYMENT_ROOT)) {
            BatchLogger.LOGGER.tracef("Processing deployment '%s' for the batch deployment resources.", deploymentUnit.getName());
            // Get the class loader
            final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
            final ClassLoader moduleClassLoader = module.getClassLoader();
            final DeploymentResourceSupport deploymentResourceSupport = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
            // Add the job operator service used interact with a deployments batch job
            final JobOperatorService jobOperatorService = new JobOperatorService(moduleClassLoader);

            // Get all the job XML service
            final JobXmlResolverService jobXmlResolverService = (JobXmlResolverService) phaseContext.getServiceRegistry().getService(BatchServiceNames.jobXmlResolverServiceName(deploymentUnit)).getValue();
            // Process each job XML file
            for (String jobXml : jobXmlResolverService.getJobXmlNames(moduleClassLoader)) {
                try {
                    final String jobName = jobXmlResolverService.resolveJobName(jobXml, moduleClassLoader);
                    // Add the job information to the service
                    jobOperatorService.addAllowedJob(jobXml, jobName);
                    // Register the a resource for each job found
                    final PathAddress jobAddress = PathAddress.pathAddress(BatchJobResourceDefinition.JOB, jobName);
                    if (!deploymentResourceSupport.hasDeploymentSubModel(BatchSubsystemDefinition.NAME, jobAddress)) {
                        deploymentResourceSupport.registerDeploymentSubResource(BatchSubsystemDefinition.NAME,
                                jobAddress, new BatchJobExecutionResource(jobOperatorService, jobName));
                    }
                } catch (Exception e) {
                    // The deployment shouldn't fail in this case, just the specific resource registration should be skipped
                    // Log a debug message so the error is not lost
                    BatchLogger.LOGGER.debugf(e, "Could not parse the XML file %s. The job will not be registered for runtime views on the deployment (%s).", jobXml, deploymentUnit.getName());
                }
            }
            phaseContext.getServiceTarget().addService(BatchServiceNames.jobOperatorServiceName(deploymentUnit), jobOperatorService)
                    .addDependency(BatchServiceNames.batchEnvironmentServiceName(deploymentUnit))
                    .install();
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
