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
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.JobExecutor;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
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
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;
import org.wildfly.extension.batch.jberet.BatchConfiguration;
import org.wildfly.extension.batch.jberet.BatchServiceNames;
import org.wildfly.extension.batch.jberet._private.BatchLogger;
import org.wildfly.extension.batch.jberet._private.Capabilities;
import org.wildfly.extension.batch.jberet.impl.BatchEnvironmentService;
import org.wildfly.extension.requestcontroller.RequestController;

/**
 * Deployment unit processor for javax.batch integration.
 */
public class BatchEnvironmentProcessor implements DeploymentUnitProcessor {

    private final boolean rcPresent;

    public BatchEnvironmentProcessor(final boolean rcPresent) {
        this.rcPresent = rcPresent;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.hasAttachment(Attachments.MODULE)) {
            BatchLogger.LOGGER.tracef("Processing deployment '%s' for the batch environment.", deploymentUnit.getName());
            // Get the class loader
            final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
            final ClassLoader moduleClassLoader = module.getClassLoader();

            final ServiceTarget serviceTarget = phaseContext.getServiceTarget();

            JobRepository jobRepository = null;
            String jobRepositoryName = null;
            String jobExecutorName = null;
            Boolean restartJobsOnResume = null;

            // Check for a deployment descriptor
            BatchEnvironmentMetaData metaData = deploymentUnit.getAttachment(BatchDeploymentDescriptorParser_1_0.ATTACHMENT_KEY);
            if (metaData == null) {
                // Check the parent
                final DeploymentUnit parent = deploymentUnit.getParent();
                if (parent != null) {
                    metaData = parent.getAttachment(BatchDeploymentDescriptorParser_1_0.ATTACHMENT_KEY);
                }
            }
            if (metaData != null) {
                jobRepository = metaData.getJobRepository();
                jobRepositoryName = metaData.getJobRepositoryName();
                jobExecutorName = metaData.getExecutorName();
                restartJobsOnResume = metaData.getRestartJobsOnResume();
            }

            final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

            // Create the batch environment
            final BatchEnvironmentService service = new BatchEnvironmentService(moduleClassLoader, WildFlyJobXmlResolver.of(moduleClassLoader, deploymentUnit), deploymentUnit.getName(), restartJobsOnResume);
            final ServiceBuilder<BatchEnvironment> serviceBuilder = serviceTarget.addService(BatchServiceNames.batchEnvironmentServiceName(deploymentUnit), service);

            // Add a dependency to the thread-pool
            if (jobExecutorName != null) {
                // Register the named thread-pool capability
                serviceBuilder.addDependency(support.getCapabilityServiceName(Capabilities.THREAD_POOL_CAPABILITY.getName(), jobExecutorName), JobExecutor.class, service.getJobExecutorInjector());
            }

            // Register the required services
            serviceBuilder.addDependency(support.getCapabilityServiceName(Capabilities.BATCH_CONFIGURATION_CAPABILITY.getName()), BatchConfiguration.class, service.getBatchConfigurationInjector());
            serviceBuilder.addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, service.getTransactionManagerInjector());

            // Register the bean manager if this is a CDI deployment
            if (WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
                BatchLogger.LOGGER.tracef("Adding BeanManager service dependency for deployment %s", deploymentUnit.getName());
                serviceBuilder.addDependency(BatchServiceNames.beanManagerServiceName(deploymentUnit), BeanManager.class, service.getBeanManagerInjector());
            }

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

            // Add the executor service for async context processing and install the service
            Services.addServerExecutorDependency(
                    serviceBuilder.addDependency(SuspendController.SERVICE_NAME, SuspendController.class, service.getSuspendControllerInjector()),
                    service.getExecutorServiceInjector(), false)
                    .install();
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
