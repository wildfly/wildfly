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

import javax.enterprise.inject.spi.BeanManager;
import javax.transaction.TransactionManager;

import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.JobExecutor;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.component.EEModuleDescription;
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
import org.wildfly.extension.batch.BatchServiceNames;
import org.wildfly.extension.batch._private.BatchLogger;
import org.wildfly.extension.batch._private.Capabilities;
import org.wildfly.extension.batch.jberet.deployment.WildFlyJobXmlResolver;
import org.wildfly.extension.batch.jberet.impl.BatchEnvironmentService;
import org.wildfly.extension.batch.job.repository.JobRepositoryFactory;
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

            final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);

            final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

            // Create the batch environment
            final BatchEnvironmentService service = new BatchEnvironmentService(moduleClassLoader, WildFlyJobXmlResolver.of(moduleClassLoader, deploymentUnit), deploymentUnit.getName());
            // Set the value for the job-repository, this can't be a capability as the JDBC job repository cannot be constructed
            // until deployment time because the default JNDI data-source name is only known during DUP processing
            service.getJobRepositoryInjector().setValue(new ImmediateValue<>(JobRepositoryFactory.getInstance().getJobRepository(moduleDescription)));
            final ServiceBuilder<BatchEnvironment> serviceBuilder = serviceTarget.addService(BatchServiceNames.batchEnvironmentServiceName(deploymentUnit), service);
            // Register the required services
            serviceBuilder.addDependency(support.getCapabilityServiceName(Capabilities.DEFAULT_THREAD_POOL_CAPABILITY.getName()), JobExecutor.class, service.getJobExecutorInjector());
            serviceBuilder.addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, service.getTransactionManagerInjector());

            // Register the bean manager if this is a CDI deployment
            if (WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
                BatchLogger.LOGGER.tracef("Adding BeanManager service dependency for deployment %s", deploymentUnit.getName());
                serviceBuilder.addDependency(BatchServiceNames.beanManagerServiceName(deploymentUnit), BeanManager.class, service.getBeanManagerInjector());
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
