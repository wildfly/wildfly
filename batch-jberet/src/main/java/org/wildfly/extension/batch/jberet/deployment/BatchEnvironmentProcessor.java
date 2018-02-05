/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.batch.jberet.deployment;

import javax.enterprise.inject.spi.BeanManager;
import javax.transaction.TransactionManager;

import org.jberet.repository.JobRepository;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.ContextClassLoaderJobOperatorContextSelector;
import org.jberet.spi.JobExecutor;
import org.jberet.spi.JobOperatorContext;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;
import org.wildfly.extension.batch.jberet.BatchConfiguration;
import org.wildfly.extension.batch.jberet.BatchServiceNames;
import org.wildfly.extension.batch.jberet._private.BatchLogger;
import org.wildfly.extension.batch.jberet._private.Capabilities;
import org.wildfly.extension.requestcontroller.RequestController;

/**
 * Deployment unit processor for javax.batch integration.
 * <p>
 * Installs the {@link BatchEnvironmentService} and {@link JobOperatorService}.
 * </p>
 */
public class BatchEnvironmentProcessor implements DeploymentUnitProcessor {

    private final boolean rcPresent;
    private final ContextClassLoaderJobOperatorContextSelector selector;

    public BatchEnvironmentProcessor(final boolean rcPresent, final ContextClassLoaderJobOperatorContextSelector selector) {
        this.rcPresent = rcPresent;
        this.selector = selector;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.hasAttachment(Attachments.MODULE)) {
            BatchLogger.LOGGER.tracef("Processing deployment '%s' for the batch environment.", deploymentUnit.getName());

            // Configure and attach the job resolver for all deployments
            final WildFlyJobXmlResolver jobXmlResolver = WildFlyJobXmlResolver.forDeployment(deploymentUnit);

            // Skip the rest of the processing for EAR's, only sub-deployments need an environment configured
            if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) return;

            // Get the class loader
            final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
            final ClassLoader moduleClassLoader = module.getClassLoader();

            final ServiceTarget serviceTarget = phaseContext.getServiceTarget();

            JobRepository jobRepository = null;
            String jobRepositoryName = null;
            String jobExecutorName = null;
            Boolean restartJobsOnResume = null;

            // Check for a deployment descriptor
            BatchEnvironmentMetaData metaData = deploymentUnit.getAttachment(BatchAttachments.BATCH_ENVIRONMENT_META_DATA);
            if (metaData == null) {
                // Check the parent
                final DeploymentUnit parent = deploymentUnit.getParent();
                if (parent != null) {
                    metaData = parent.getAttachment(BatchAttachments.BATCH_ENVIRONMENT_META_DATA);
                }
            }
            if (metaData != null) {
                jobRepository = metaData.getJobRepository();
                jobRepositoryName = metaData.getJobRepositoryName();
                jobExecutorName = metaData.getExecutorName();
                restartJobsOnResume = metaData.getRestartJobsOnResume();
            }

            final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

            final String deploymentName = deploymentUnit.getName();

            // Create the job operator service used interact with a deployments batch job
            final JobOperatorService jobOperatorService = new JobOperatorService(restartJobsOnResume, deploymentName, jobXmlResolver);

            // Create the batch environment
            final BatchEnvironmentService service = new BatchEnvironmentService(moduleClassLoader, jobXmlResolver, deploymentName);
            final ServiceBuilder<SecurityAwareBatchEnvironment> serviceBuilder = serviceTarget.addService(BatchServiceNames.batchEnvironmentServiceName(deploymentUnit), service);

            // Add a dependency to the thread-pool
            if (jobExecutorName != null) {
                // Register the named thread-pool capability
                serviceBuilder.addDependency(support.getCapabilityServiceName(Capabilities.THREAD_POOL_CAPABILITY.getName(), jobExecutorName), JobExecutor.class, service.getJobExecutorInjector());
            }

            // Register the required services
            serviceBuilder.addDependency(support.getCapabilityServiceName(Capabilities.BATCH_CONFIGURATION_CAPABILITY.getName()), BatchConfiguration.class, service.getBatchConfigurationInjector());
            serviceBuilder.addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, service.getTransactionManagerInjector());

            final ServiceName artifactFactoryServiceName = BatchServiceNames.batchArtifactFactoryServiceName(deploymentUnit);
            final ArtifactFactoryService artifactFactoryService = new ArtifactFactoryService();
            final ServiceBuilder<ArtifactFactory> artifactFactoryServiceBuilder = serviceTarget.addService(artifactFactoryServiceName, artifactFactoryService);

            // Register the bean manager if this is a CDI deployment
            if (WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
                BatchLogger.LOGGER.tracef("Adding BeanManager service dependency for deployment %s", deploymentUnit.getName());
                artifactFactoryServiceBuilder.addDependency(BatchServiceNames.beanManagerServiceName(deploymentUnit), BeanManager.class,
                        artifactFactoryService.getBeanManagerInjector());
            }
            artifactFactoryServiceBuilder.install();
            serviceBuilder.addDependency(artifactFactoryServiceName, WildFlyArtifactFactory.class, service.getArtifactFactoryInjector());

            // No deployment defined repository, use the default
            if (jobRepositoryName != null) {
                // Register a named job repository
                serviceBuilder.addDependency(support.getCapabilityServiceName(Capabilities.JOB_REPOSITORY_CAPABILITY.getName(), jobRepositoryName), JobRepository.class, service.getJobRepositoryInjector());
            } else {
                // Use the job repository as defined in the deployment descriptor
                service.getJobRepositoryInjector().setValue(new ImmediateValue<>(jobRepository));
            }

            if (rcPresent) {
                serviceBuilder.addDependency(RequestController.SERVICE_NAME, RequestController.class, service.getRequestControllerInjector());
            }

            // Install the batch environment service
            serviceBuilder.install();

            // Install the JobOperatorService
            Services.addServerExecutorDependency(serviceTarget.addService(BatchServiceNames.jobOperatorServiceName(deploymentUnit), jobOperatorService)
                            .addDependency(support.getCapabilityServiceName(Capabilities.BATCH_CONFIGURATION_CAPABILITY.getName()), BatchConfiguration.class, jobOperatorService.getBatchConfigurationInjector())
                            .addDependency(SuspendController.SERVICE_NAME, SuspendController.class, jobOperatorService.getSuspendControllerInjector())
                            .addDependency(BatchServiceNames.batchEnvironmentServiceName(deploymentUnit), SecurityAwareBatchEnvironment.class, jobOperatorService.getBatchEnvironmentInjector()),
                    jobOperatorService.getExecutorServiceInjector())
                    .install();

            // Add the JobOperatorService to the deployment unit
            deploymentUnit.putAttachment(BatchAttachments.JOB_OPERATOR, jobOperatorService);

            // Add the JobOperator to the context selector
            selector.registerContext(moduleClassLoader, JobOperatorContext.create(jobOperatorService));
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        if (context.hasAttachment(Attachments.MODULE)) {
            selector.unregisterContext(context.getAttachment(Attachments.MODULE).getClassLoader());
        }
    }
}
