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

import static org.jboss.as.server.deployment.Attachments.DEPLOYMENT_COMPLETE_SERVICES;
import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.sql.DataSource;

import jakarta.batch.operations.JobOperator;
import jakarta.enterprise.inject.spi.BeanManager;
import org.jberet.repository.JobRepository;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.ContextClassLoaderJobOperatorContextSelector;
import org.jberet.spi.JobExecutor;
import org.jberet.spi.JobOperatorContext;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.as.weld.WeldCapability;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.batch.jberet.BatchConfiguration;
import org.wildfly.extension.batch.jberet.BatchServiceNames;
import org.wildfly.extension.batch.jberet._private.BatchLogger;
import org.wildfly.extension.batch.jberet._private.Capabilities;
import org.wildfly.extension.batch.jberet.job.repository.JdbcJobRepositoryService;
import org.wildfly.extension.requestcontroller.RequestController;

/**
 * Deployment unit processor for jakarta.batch integration.
 * <p>
 * Installs the {@link BatchEnvironmentService} and {@link JobOperatorService}.
 * </p>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
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

            // Check for a deployment descriptor
            BatchEnvironmentMetaData metaData = deploymentUnit.getAttachment(BatchAttachments.BATCH_ENVIRONMENT_META_DATA);
            if (metaData == null) {
                // Check the parent
                final DeploymentUnit parent = deploymentUnit.getParent();
                if (parent != null) {
                    metaData = parent.getAttachment(BatchAttachments.BATCH_ENVIRONMENT_META_DATA);
                }
            }
            final JobRepository jobRepository = metaData != null ? metaData.getJobRepository() : null;
            final String jobRepositoryName = metaData != null ? metaData.getJobRepositoryName() : null;
            final String dataSourceName = metaData != null ? metaData.getDataSourceName() : null;
            final String jobExecutorName = metaData != null ? metaData.getExecutorName() : null;
            final Boolean restartJobsOnResume = metaData != null ? metaData.getRestartJobsOnResume() : null;
            final Integer executionRecordsLimit = metaData != null ? metaData.getExecutionRecordsLimit() : null;

            final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

            final String deploymentName = deploymentUnit.getName();

            // Create the batch environment
            final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            final NamespaceContextSelector namespaceContextSelector = eeModuleDescription == null ? null : eeModuleDescription.getNamespaceContextSelector();
            final ServiceName batchEnvSN = BatchServiceNames.batchEnvironmentServiceName(deploymentUnit);
            final ServiceBuilder<?> serviceBuilder = serviceTarget.addService(batchEnvSN);
            final Consumer<SecurityAwareBatchEnvironment> batchEnvironmentConsumer = serviceBuilder.provides(batchEnvSN);
            // Add a dependency to the thread-pool
            final Supplier<JobExecutor> jobExecutorSupplier = jobExecutorName != null ? serviceBuilder.requires(Capabilities.THREAD_POOL_CAPABILITY.getCapabilityServiceName(jobExecutorName)) : null;
            // Register the required services
            final Supplier<BatchConfiguration> batchConfigurationSupplier = serviceBuilder.requires(Capabilities.BATCH_CONFIGURATION_CAPABILITY.getCapabilityServiceName());
            // Ensure local transaction support is started
            serviceBuilder.requires(support.getCapabilityServiceName(Capabilities.LOCAL_TRANSACTION_PROVIDER_CAPABILITY));

            final ServiceName artifactFactoryServiceName = BatchServiceNames.batchArtifactFactoryServiceName(deploymentUnit);
            final ServiceBuilder<?> artifactFactoryServiceBuilder = serviceTarget.addService(artifactFactoryServiceName);
            final Consumer<ArtifactFactory> artifactFactoryConsumer = artifactFactoryServiceBuilder.provides(artifactFactoryServiceName);
            Supplier<BeanManager> beanManagerSupplier = null;
            // Register the bean manager if this is a Jakarta Contexts and Dependency Injection deployment
            if (support.hasCapability(WELD_CAPABILITY_NAME)) {
                final WeldCapability api = support.getOptionalCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class).get();
                if (api.isPartOfWeldDeployment(deploymentUnit)) {
                    BatchLogger.LOGGER.tracef("Adding BeanManager service dependency for deployment %s", deploymentUnit.getName());
                    beanManagerSupplier = api.addBeanManagerService(deploymentUnit, artifactFactoryServiceBuilder);
                }
            }
            final ArtifactFactoryService artifactFactoryService = new ArtifactFactoryService(artifactFactoryConsumer, beanManagerSupplier);
            artifactFactoryServiceBuilder.setInstance(artifactFactoryService);
            artifactFactoryServiceBuilder.install();

            final Supplier<WildFlyArtifactFactory> artifactFactorySupplier = serviceBuilder.requires(artifactFactoryServiceName);
            Supplier<JobRepository> jobRepositorySupplier = null;
            if (jobRepositoryName != null) {
                // Register a named job repository
                jobRepositorySupplier = serviceBuilder.requires(support.getCapabilityServiceName(Capabilities.JOB_REPOSITORY_CAPABILITY.getName(), jobRepositoryName));
            } else if (dataSourceName != null) {
                // Register a jdbc job repository with data-source
                final ServiceName jobRepositoryServiceName = support.getCapabilityServiceName(Capabilities.JOB_REPOSITORY_CAPABILITY.getName(), deploymentName);
                final ServiceBuilder<?> jobRepositoryServiceBuilder = serviceTarget.addService(jobRepositoryServiceName);
                final Consumer<JobRepository> jobRepositoryConsumer = jobRepositoryServiceBuilder.provides(jobRepositoryServiceName);
                final Supplier<ExecutorService> executorSupplier = Services.requireServerExecutor(jobRepositoryServiceBuilder);
                final Supplier<DataSource> dataSourceSupplier = jobRepositoryServiceBuilder.requires(support.getCapabilityServiceName(Capabilities.DATA_SOURCE_CAPABILITY, dataSourceName));
                final JdbcJobRepositoryService jdbcJobRepositoryService = new JdbcJobRepositoryService(jobRepositoryConsumer, dataSourceSupplier, executorSupplier, executionRecordsLimit);
                jobRepositoryServiceBuilder.setInstance(jdbcJobRepositoryService);
                jobRepositoryServiceBuilder.install();
                jobRepositorySupplier = serviceBuilder.requires(jobRepositoryServiceName);
            } else if (jobRepository != null) {
                // Use the job repository as defined in the deployment descriptor
                jobRepositorySupplier = () -> jobRepository;
            }

            final Supplier<RequestController> requestControllerSupplier = rcPresent ? serviceBuilder.requires(RequestController.SERVICE_NAME) : null;

            // Install the batch environment service
            final BatchEnvironmentService service = new BatchEnvironmentService(batchEnvironmentConsumer, artifactFactorySupplier, jobExecutorSupplier, requestControllerSupplier, jobRepositorySupplier, batchConfigurationSupplier, moduleClassLoader, jobXmlResolver, deploymentName, namespaceContextSelector);
            serviceBuilder.setInstance(service);
            serviceBuilder.install();

            // Install the JobOperatorService
            final ServiceName jobOperatorServiceName = BatchServiceNames.jobOperatorServiceName(deploymentUnit);
            final ServiceBuilder<?> jobOperatorServiceSB = serviceTarget.addService(jobOperatorServiceName);
            final Consumer<JobOperator> jobOperatorConsumer = jobOperatorServiceSB.provides(jobOperatorServiceName);
            final Supplier<ExecutorService> executorSupplier = Services.requireServerExecutor(jobOperatorServiceSB);
            final Supplier<BatchConfiguration> batchConfigSupplier = jobOperatorServiceSB.requires(support.getCapabilityServiceName(Capabilities.BATCH_CONFIGURATION_CAPABILITY.getName()));
            final Supplier<SuspendController> suspendControllerSupplier = jobOperatorServiceSB.requires(support.getCapabilityServiceName(Capabilities.SUSPEND_CONTROLLER_CAPABILITY));
            final Supplier<ProcessStateNotifier> processStateSupplier = jobOperatorServiceSB.requires(support.getCapabilityServiceName(Capabilities.PROCESS_STATE_NOTIFIER_CAPABILITY));
            final Supplier<SecurityAwareBatchEnvironment> batchEnvironmentSupplier = jobOperatorServiceSB.requires(BatchServiceNames.batchEnvironmentServiceName(deploymentUnit));
            final JobOperatorService jobOperatorService = new JobOperatorService(jobOperatorConsumer, batchConfigSupplier, batchEnvironmentSupplier, executorSupplier, suspendControllerSupplier, processStateSupplier, restartJobsOnResume, deploymentName, jobXmlResolver);
            jobOperatorServiceSB.setInstance(jobOperatorService);
            jobOperatorServiceSB.install();

            // Add the JobOperatorService to the deployment unit
            deploymentUnit.putAttachment(BatchAttachments.JOB_OPERATOR, jobOperatorService);
            deploymentUnit.addToAttachmentList(DEPLOYMENT_COMPLETE_SERVICES, jobOperatorServiceName);

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
