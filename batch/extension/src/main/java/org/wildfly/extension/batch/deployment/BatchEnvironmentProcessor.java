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

package org.wildfly.extension.batch.deployment;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import javax.enterprise.inject.spi.BeanManager;
import javax.transaction.TransactionManager;

import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.JobXmlResolver;
import org.jboss.as.ee.component.EEModuleDescription;
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
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.wildfly.extension.batch.BatchServiceNames;
import org.wildfly.extension.batch._private.BatchLogger;
import org.wildfly.extension.batch.jberet.deployment.JobXmlResolverService;
import org.wildfly.extension.batch.job.repository.JobRepositoryFactory;
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

            final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);

            // Create the batch environment
            final BatchEnvironmentService service = new BatchEnvironmentService(moduleClassLoader, deploymentUnit.getName());
            // Set the value for the job-repository, this can't be a capability as the JDBC job repository cannot be constructed
            // until deployment time because the default JNDI data-source name is only known during DUP processing
            service.getJobRepositoryInjector().setValue(new ImmediateValue<>(JobRepositoryFactory.getInstance().getJobRepository(moduleDescription)));
            final ServiceBuilder<BatchEnvironment> serviceBuilder = serviceTarget.addService(BatchServiceNames.batchEnvironmentServiceName(deploymentUnit), service);
            // Register the required services
            serviceBuilder.addDependency(BatchServiceNames.BATCH_THREAD_POOL_NAME, ExecutorService.class, service.getExecutorServiceInjector());
            serviceBuilder.addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, service.getTransactionManagerInjector());

            // Register the bean manager if this is a CDI deployment
            if (WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
                BatchLogger.LOGGER.tracef("Adding BeanManager service dependency for deployment %s", deploymentUnit.getName());
                serviceBuilder.addDependency(BatchServiceNames.beanManagerServiceName(deploymentUnit), BeanManager.class, service.getBeanManagerInjector());
            }

            // Get the root file
            final VirtualFile root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
            VirtualFile jobsDir = null;
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
            Services.addServerExecutorDependency(
                    serviceTarget.addService(org.wildfly.extension.batch.jberet.BatchServiceNames.jobXmlResolverServiceName(deploymentUnit), jobXmlResolverService),
                    jobXmlResolverService.getExecutorServiceInjector(), false)
                    .install();
            // Add a dependency to the job XML resolver service
            serviceBuilder.addDependency(BatchServiceNames.jobXmlResolverServiceName(deploymentUnit), JobXmlResolver.class, service.getJobXmlResolverInjector());

            if (rcPresent) {
                serviceBuilder.addDependency(RequestController.SERVICE_NAME, RequestController.class, service.getRequestControllerInjector());
            }

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
