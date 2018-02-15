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

package org.wildfly.extension.batch.jberet.deployment;

import java.util.Properties;
import javax.transaction.TransactionManager;

import org.jberet.repository.JobRepository;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.JobExecutor;
import org.jberet.spi.JobTask;
import org.jberet.spi.JobXmlResolver;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.batch.jberet.BatchConfiguration;
import org.wildfly.extension.batch.jberet._private.BatchLogger;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RequestController;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchEnvironmentService implements Service<SecurityAwareBatchEnvironment> {

    private static final Properties PROPS = new Properties();

    private final InjectedValue<WildFlyArtifactFactory> artifactFactoryInjector = new InjectedValue<>();
    private final InjectedValue<JobExecutor> jobExecutorInjector = new InjectedValue<>();
    private final InjectedValue<TransactionManager> transactionManagerInjector = new InjectedValue<>();
    private final InjectedValue<RequestController> requestControllerInjector = new InjectedValue<>();
    private final InjectedValue<JobRepository> jobRepositoryInjector = new InjectedValue<>();
    private final InjectedValue<BatchConfiguration> batchConfigurationInjector = new InjectedValue<>();

    private final ClassLoader classLoader;
    private final JobXmlResolver jobXmlResolver;
    private final String deploymentName;
    private SecurityAwareBatchEnvironment batchEnvironment = null;
    private volatile ControlPoint controlPoint;

    public BatchEnvironmentService(final ClassLoader classLoader, final JobXmlResolver jobXmlResolver, final String deploymentName) {
        this.classLoader = classLoader;
        this.jobXmlResolver = jobXmlResolver;
        this.deploymentName = deploymentName;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        BatchLogger.LOGGER.debugf("Creating batch environment; %s", classLoader);
        final BatchConfiguration batchConfiguration = batchConfigurationInjector.getValue();
        // Find the job executor to use
        JobExecutor jobExecutor = jobExecutorInjector.getOptionalValue();
        if (jobExecutor == null) {
            jobExecutor = batchConfiguration.getDefaultJobExecutor();
        }
        // Find the job repository to use
        JobRepository jobRepository = jobRepositoryInjector.getOptionalValue();
        if (jobRepository == null) {
            jobRepository = batchConfiguration.getDefaultJobRepository();
        }

        this.batchEnvironment = new WildFlyBatchEnvironment(artifactFactoryInjector.getValue(),
                jobExecutor, transactionManagerInjector.getValue(),
                jobRepository, jobXmlResolver);

        final RequestController requestController = requestControllerInjector.getOptionalValue();
        if (requestController != null) {
            // Create the entry point
            controlPoint = requestController.getControlPoint(deploymentName, "batch-executor-service");
        } else {
            controlPoint = null;
        }
    }

    @Override
    public synchronized void stop(final StopContext context) {
        BatchLogger.LOGGER.debugf("Removing batch environment; %s", classLoader);
        batchEnvironment = null;
        if (controlPoint != null) {
            requestControllerInjector.getValue().removeControlPoint(controlPoint);
        }
    }

    @Override
    public synchronized SecurityAwareBatchEnvironment getValue() throws IllegalStateException, IllegalArgumentException {
        return batchEnvironment;
    }

    public InjectedValue<WildFlyArtifactFactory> getArtifactFactoryInjector() {
        return artifactFactoryInjector;
    }

    public InjectedValue<JobExecutor> getJobExecutorInjector() {
        return jobExecutorInjector;
    }

    public InjectedValue<TransactionManager> getTransactionManagerInjector() {
        return transactionManagerInjector;
    }

    public InjectedValue<RequestController> getRequestControllerInjector() {
        return requestControllerInjector;
    }

    public InjectedValue<JobRepository> getJobRepositoryInjector() {
        return jobRepositoryInjector;
    }

    public InjectedValue<BatchConfiguration> getBatchConfigurationInjector() {
        return batchConfigurationInjector;
    }

    private class WildFlyBatchEnvironment implements BatchEnvironment, SecurityAwareBatchEnvironment {

        private final WildFlyArtifactFactory artifactFactory;
        private final JobExecutor jobExecutor;
        private final TransactionManager transactionManager;
        private final JobRepository jobRepository;
        private final JobXmlResolver jobXmlResolver;

        WildFlyBatchEnvironment(final WildFlyArtifactFactory artifactFactory,
                                final JobExecutor jobExecutor,
                                final TransactionManager transactionManager,
                                final JobRepository jobRepository,
                                final JobXmlResolver jobXmlResolver) {
            this.jobXmlResolver = jobXmlResolver;
            this.artifactFactory = artifactFactory;
            this.jobExecutor = jobExecutor;
            this.transactionManager = transactionManager;
            this.jobRepository = jobRepository;
        }

        @Override
        public ClassLoader getClassLoader() {
            return classLoader;
        }

        @Override
        public ArtifactFactory getArtifactFactory() {
            return artifactFactory;
        }

        @Override
        public void submitTask(final JobTask jobTask) {
            final SecurityIdentity identity = getIdentity();
            final ContextHandle contextHandle = createContextHandle();
            final JobTask task = new JobTask() {
                @Override
                public int getRequiredRemainingPermits() {
                    return jobTask.getRequiredRemainingPermits();
                }

                @Override
                public void run() {
                    final ContextHandle.Handle handle = contextHandle.setup();
                    try {
                        if (identity == null) {
                            jobTask.run();
                        } else {
                            identity.runAs(jobTask);
                        }
                    } finally {
                        handle.tearDown();
                    }
                }
            };
            if (controlPoint == null) {
                jobExecutor.execute(task);
            } else {
                // Queue the task to run in the control point, if resume is executed the queued tasks will run
                controlPoint.queueTask(task, jobExecutor, -1, null, false);
            }
        }

        @Override
        public TransactionManager getTransactionManager() {
            return transactionManager;
        }

        @Override
        public JobRepository getJobRepository() {
            return jobRepository;
        }

        @Override
        public JobXmlResolver getJobXmlResolver() {
            return jobXmlResolver;
        }

        @Override
        public Properties getBatchConfigurationProperties() {
            return PROPS;
        }

        @Override
        public SecurityDomain getSecurityDomain() {
            return batchConfigurationInjector.getValue().getSecurityDomain();
        }

        private ContextHandle createContextHandle() {
            final ClassLoader tccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            // If the TCCL is null, use the deployments ModuleClassLoader
            final ClassLoaderContextHandle classLoaderContextHandle = (tccl == null ? new ClassLoaderContextHandle(classLoader) : new ClassLoaderContextHandle(tccl));
            // Class loader handle must be first so the TCCL is set before the other handles execute
            return new ContextHandle.ChainedContextHandle(classLoaderContextHandle, new NamespaceContextHandle(),
                    new SecurityContextHandle(), artifactFactory.createContextHandle());
        }
    }
}