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

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import javax.enterprise.inject.spi.BeanManager;
import javax.transaction.UserTransaction;

import org.jberet.spi.BatchEnvironment;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.modules.Module;
import org.jboss.msc.inject.CastingInjector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.batch._private.BatchLogger;
import org.wildfly.extension.batch.services.BatchServiceNames;
import org.wildfly.jberet.BatchEnvironmentFactory;
import org.wildfly.jberet.services.BatchEnvironmentService;

/**
 * Deployment unit processor for javax.batch integration.
 */
public class BatchEnvironmentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        // Can't use batch without CDI
        if (deploymentUnit.hasAttachment(Attachments.MODULE) && deploymentUnit.hasAttachment(Attachments.DEPLOYMENT_ROOT)) {
            if (WeldDeploymentMarker.isWeldDeployment(deploymentUnit)) {
                // Get the class loader
                final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
                final ClassLoader moduleClassLoader = module.getClassLoader();

                final ServiceTarget serviceTarget = phaseContext.getServiceTarget();

                final BatchEnvironmentService service = new BatchEnvironmentService();
                // Add the service to the factory
                BatchEnvironmentFactory.getInstance().add(moduleClassLoader, service);

                final ServiceBuilder<BatchEnvironment> serviceBuilder = serviceTarget.addService(BatchServiceNames.batchDeploymentServiceName(deploymentUnit), service);
                serviceBuilder.addDependency(BatchServiceNames.BATCH_SERVICE_NAME, Properties.class, service.getPropertiesInjector());
                serviceBuilder.addDependency(TxnServices.JBOSS_TXN_USER_TRANSACTION, UserTransaction.class, service.getUserTransactionInjector());
                serviceBuilder.addDependency(BatchServiceNames.BATCH_THREAD_POOL_NAME, ExecutorService.class, service.getExecutorServiceInjector());
                // TODO (jrp) remove this, but it's only one that currently works
                // serviceBuilder.addDependency(ConcurrentServiceNames.DEFAULT_MANAGED_EXECUTOR_SERVICE_SERVICE_NAME, ExecutorService.class, service.getExecutorServiceInjector());

                // Set the class loader
                service.getClassLoaderInjector().setValue(new ImmediateValue<ClassLoader>(moduleClassLoader));

                // Add the bean manager
                serviceBuilder.addDependency(BatchServiceNames.beanManagerServiceName(deploymentUnit), new CastingInjector<BeanManager>(service.getBeanManagerInjector(), BeanManager.class));

                serviceBuilder.install();
            } else {
                final ResourceRoot root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
                final VirtualFile dir = root.getRoot().getChild("META-INF/batch-jobs");
                if (dir.exists() && !dir.getChildren().isEmpty()) {
                    BatchLogger.LOGGER.cdiNotEnabled();
                }

            }
        }
        // TODO (jrp) could produce NPE with the BatchEnvironment if the MODULE wasn't attached
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        if (context.hasAttachment(Attachments.MODULE)) {
            // Get the class loader
            final Module module = context.getAttachment(Attachments.MODULE);
            final ClassLoader moduleClassLoader = module.getClassLoader();
            // Remove the service to the factory
            BatchEnvironmentFactory.getInstance().remove(moduleClassLoader);
        }
    }
}
