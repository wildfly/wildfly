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
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.jberet.repository.JobRepository;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.JobXmlResolver;
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
import org.jboss.as.txn.service.TxnServices;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.wildfly.extension.batch.jberet.BatchServiceNames;
import org.wildfly.extension.batch.jberet._private.BatchLogger;
import org.wildfly.extension.batch.jberet._private.Capabilities;
import org.wildfly.extension.requestcontroller.RequestController;
import org.wildfly.jberet.services.BatchEnvironmentService;

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
            ExecutorService executorService = null;

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
                executorService = metaData.getExecutorService();
            }

            final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

            // Create the batch environment
            final BatchEnvironmentService service = new BatchEnvironmentService(moduleClassLoader, deploymentUnit.getName());
            final ServiceBuilder<BatchEnvironment> serviceBuilder = serviceTarget.addService(BatchServiceNames.batchEnvironmentServiceName(deploymentUnit), service);

            // Add a dependency to the thread-pool
            if (executorService == null) {
                // Use the default
                serviceBuilder.addDependency(support.getCapabilityServiceName(Capabilities.DEFAULT_THREAD_POOL_CAPABILITY.getName()), ExecutorService.class, service.getExecutorServiceInjector());
            } else {
                // Use the thread pool from the deployment descriptor
                service.getExecutorServiceInjector().setValue(new ImmediateValue<>(executorService));
            }

            // Register the required services
            serviceBuilder.addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, service.getTransactionManagerInjector());

            // Register the bean manager if this is a CDI deployment
            if (WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
                BatchLogger.LOGGER.tracef("Adding BeanManager service dependency for deployment %s", deploymentUnit.getName());
                serviceBuilder.addDependency(BatchServiceNames.beanManagerServiceName(deploymentUnit), BeanManager.class, service.getBeanManagerInjector());
            }

            // Get the root file
            final VirtualFile root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
            VirtualFile jobsDir;
            // Only files in the META-INF/batch-jobs directory
            if (DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
                jobsDir = root.getChild("WEB-INF/classes/META-INF/batch-jobs");
            } else {
                jobsDir = root.getChild("META-INF/batch-jobs");
            }
            final JobXmlResolverService jobXmlResolverService;
            if (jobsDir != null && jobsDir.exists()) {
                try {
                    // Create the job XML resolver service with the files allowed to be used
                    jobXmlResolverService = new JobXmlResolverService(moduleClassLoader, jobsDir.getChildren(JobXmlFilter.INSTANCE));
                } catch (IOException e) {
                    throw BatchLogger.LOGGER.errorProcessingBatchJobsDir(e);
                }
            } else {
                // This is likely not a batch deployment, creates a no-op service
                jobXmlResolverService = new JobXmlResolverService();
            }
            // Install the job XML resolver service
            final ServiceBuilder<JobXmlResolver> jobXmlServiceBuilder = Services.addServerExecutorDependency(
                    serviceTarget.addService(BatchServiceNames.jobXmlResolverServiceName(deploymentUnit), jobXmlResolverService),
                    jobXmlResolverService.getExecutorServiceInjector(), false);
            // Add a dependency to the job XML resolver service
            serviceBuilder.addDependency(BatchServiceNames.jobXmlResolverServiceName(deploymentUnit), JobXmlResolver.class, service.getJobXmlResolverInjector());

            // No deployment defined repository, use the default
            if (jobRepository == null) {
                final ServiceName defaultJobRepository = support.getCapabilityServiceName(Capabilities.DEFAULT_JOB_REPOSITORY_CAPABILITY.getName());
                serviceBuilder.addDependency(defaultJobRepository, JobRepository.class, service.getJobRepositoryInjector());
            } else {
                service.getJobRepositoryInjector().setValue(new ImmediateValue<>(jobRepository));
            }

            if (rcPresent) {
                serviceBuilder.addDependency(RequestController.SERVICE_NAME, RequestController.class, service.getRequestControllerInjector());
            }

            jobXmlServiceBuilder.install();
            serviceBuilder.install();
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private static class JobXmlFilter implements VirtualFileFilter {

        static final JobXmlFilter INSTANCE = new JobXmlFilter();

        @Override
        public boolean accepts(final VirtualFile file) {
            return file.isFile() && file.getName().endsWith(".xml");
        }
    }
}
