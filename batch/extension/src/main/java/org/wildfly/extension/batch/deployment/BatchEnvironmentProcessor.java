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

import java.util.concurrent.ExecutorService;
import javax.enterprise.inject.spi.BeanManager;
import javax.transaction.TransactionManager;

import org.jberet.spi.BatchEnvironment;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
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
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.batch.BatchServiceNames;
import org.wildfly.extension.batch._private.BatchLogger;
import org.wildfly.extension.batch.job.repository.JobRepositoryFactory;
import org.wildfly.jberet.services.BatchEnvironmentService;

/**
 * Deployment unit processor for javax.batch integration.
 */
public class BatchEnvironmentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.hasAttachment(Attachments.MODULE)) {
            BatchLogger.LOGGER.tracef("Processing deployment '%s' for batch.", deploymentUnit.getName());
            // Get the class loader
            final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
            final ClassLoader moduleClassLoader = module.getClassLoader();

            final ServiceTarget serviceTarget = phaseContext.getServiceTarget();

            final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);

            final BatchEnvironmentService service = new BatchEnvironmentService(moduleClassLoader, JobRepositoryFactory.getInstance().getJobRepository(moduleDescription));
            final ServiceBuilder<BatchEnvironment> serviceBuilder = serviceTarget.addService(BatchServiceNames.batchDeploymentServiceName(deploymentUnit), service);
            serviceBuilder.addDependency(BatchServiceNames.BATCH_THREAD_POOL_NAME, ExecutorService.class, service.getExecutorServiceInjector());

            // Only add transactions and the BeanManager if this is a batch deployment
            if (isBatchDeployment(deploymentUnit)) {
                BatchLogger.LOGGER.tracef("Adding UserTransaction and BeanManager service dependencies for deployment %s", deploymentUnit.getName());
                serviceBuilder.addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, service.getTransactionManagerInjector());

                // Add the bean manager
                serviceBuilder.addDependency(BatchServiceNames.beanManagerServiceName(deploymentUnit), new CastingInjector<>(service.getBeanManagerInjector(), BeanManager.class));
            } else {
                BatchLogger.LOGGER.tracef("Skipping UserTransaction and BeanManager service dependencies for deployment %s", deploymentUnit.getName());
            }

            serviceBuilder.install();
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    /**
     * Batch deployments must have a {@code META-INF/batch.xml} and/or XML configuration files in {@code
     * META-INF/batch-jobs}. They must be in an EJB JAR or a WAR.
     *
     * @param deploymentUnit the deployment unit to check
     *
     * @return {@code true} if a {@code META-INF/batch.xml} or a non-empty {@code META-INF/batch-jobs} directory was
     * found otherwise {@code false}
     */
    private boolean isBatchDeployment(final DeploymentUnit deploymentUnit) {
        // Section 10.7 of JSR 352 discusses valid packaging types, of which it appears EAR should be one. It seems
        // though that it's of no real use as 10.5 and 10.6 seem to indicate it must be in META-INF/batch-jobs of a JAR
        // and WEB-INF/classes/META-INF/batch-jobs of a WAR.
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit) || !deploymentUnit.hasAttachment(Attachments.DEPLOYMENT_ROOT)) {
            return false;
        }
        final ResourceRoot root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final VirtualFile metaInf;
        if (DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            metaInf = root.getRoot().getChild("WEB-INF/classes/META-INF");
        } else {
            metaInf = root.getRoot().getChild("META-INF");
        }
        final VirtualFile jobXmlFile = metaInf.getChild("batch.xml");
        final VirtualFile batchJobsDir = metaInf.getChild("batch-jobs");
        return (jobXmlFile.exists() || (batchJobsDir.exists() && !batchJobsDir.getChildren().isEmpty()));
    }
}
