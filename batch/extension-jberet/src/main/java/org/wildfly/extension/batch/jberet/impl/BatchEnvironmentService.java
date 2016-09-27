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

package org.wildfly.extension.batch.jberet.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.enterprise.inject.spi.BeanManager;
import javax.transaction.TransactionManager;

import org.jberet.repository.JobRepository;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.JobExecutor;
import org.jberet.spi.JobTask;
import org.jberet.spi.JobXmlResolver;
import org.jboss.as.server.suspend.ServerActivity;
import org.jboss.as.server.suspend.ServerActivityCallback;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.batch.jberet.BatchConfiguration;
import org.wildfly.extension.batch.jberet._private.BatchLogger;
import org.wildfly.extension.batch.jberet.impl.ContextHandle.ChainedContextHandle;
import org.wildfly.extension.batch.jberet.impl.ContextHandle.Handle;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RequestController;
import org.wildfly.jberet.BatchEnvironmentFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchEnvironmentService implements Service<BatchEnvironment> {

    // This can be removed after the getBatchConfigurationProperties() is removed from jBeret
    private static final Properties PROPS = new Properties();
    private static final Properties RESTART_PROPS = new Properties();

    private final InjectedValue<BeanManager> beanManagerInjector = new InjectedValue<>();
    private final InjectedValue<JobExecutor> jobExecutorInjector = new InjectedValue<>();
    private final InjectedValue<TransactionManager> transactionManagerInjector = new InjectedValue<>();
    private final InjectedValue<RequestController> requestControllerInjector = new InjectedValue<>();
    private final InjectedValue<JobRepository> jobRepositoryInjector = new InjectedValue<>();
    private final InjectedValue<ExecutorService> executorInjector = new InjectedValue<>();
    private final InjectedValue<SuspendController> suspendControllerInjector = new InjectedValue<>();
    private final InjectedValue<BatchConfiguration> batchConfigurationInjector = new InjectedValue<>();

    private final ClassLoader classLoader;
    private final JobXmlResolver jobXmlResolver;
    private final String deploymentName;
    private final BatchJobServerActivity serverActivity;
    private final Boolean restartJobsOnResume;
    private BatchEnvironment batchEnvironment = null;
    private volatile ControlPoint controlPoint;

    public BatchEnvironmentService(final ClassLoader classLoader, final JobXmlResolver jobXmlResolver, final String deploymentName) {
        this(classLoader, jobXmlResolver, deploymentName, null);
    }

    public BatchEnvironmentService(final ClassLoader classLoader, final JobXmlResolver jobXmlResolver, final String deploymentName, final Boolean restartJobsOnResume) {
        this.classLoader = classLoader;
        this.jobXmlResolver = jobXmlResolver;
        this.deploymentName = deploymentName;
        this.serverActivity = new BatchJobServerActivity();
        this.restartJobsOnResume = restartJobsOnResume;
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

        final BatchEnvironment batchEnvironment = new WildFlyBatchEnvironment(beanManagerInjector.getOptionalValue(),
                jobExecutor, transactionManagerInjector.getValue(),
                jobRepository, jobXmlResolver);
        // Add the service to the factory
        BatchEnvironmentFactory.getInstance().add(classLoader, batchEnvironment);
        this.batchEnvironment = batchEnvironment;

        suspendControllerInjector.getValue().registerActivity(serverActivity);
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
        // Remove the server activity
        suspendControllerInjector.getValue().unRegisterActivity(serverActivity);
        final ExecutorService service = executorInjector.getValue();

        final Runnable task = () -> {
            // Should already be stopped, but just to be safe we'll make one more attempt
            serverActivity.stopRunningJobs(false);
            // Remove this instance from the factory
            BatchEnvironmentFactory.getInstance().remove(classLoader);
            batchEnvironment = null;
            if (controlPoint != null) {
                requestControllerInjector.getValue().removeControlPoint(controlPoint);
            }
            context.complete();
        };
        try {
            service.execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            context.asynchronous();
        }
    }

    @Override
    public synchronized BatchEnvironment getValue() throws IllegalStateException, IllegalArgumentException {
        return batchEnvironment;
    }

    public InjectedValue<BeanManager> getBeanManagerInjector() {
        return beanManagerInjector;
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

    public Injector<ExecutorService> getExecutorServiceInjector() {
        return executorInjector;
    }

    public InjectedValue<SuspendController> getSuspendControllerInjector() {
        return suspendControllerInjector;
    }

    public InjectedValue<BatchConfiguration> getBatchConfigurationInjector() {
        return batchConfigurationInjector;
    }

    private class WildFlyBatchEnvironment implements BatchEnvironment {

        private final ArtifactFactory artifactFactory;
        private final JobExecutor jobExecutor;
        private final TransactionManager transactionManager;
        private final JobRepository jobRepository;
        private final JobXmlResolver jobXmlResolver;

        WildFlyBatchEnvironment(final BeanManager beanManager,
                                final JobExecutor jobExecutor,
                                final TransactionManager transactionManager,
                                final JobRepository jobRepository,
                                final JobXmlResolver jobXmlResolver) {
            this.jobXmlResolver = jobXmlResolver;
            artifactFactory = new WildFlyArtifactFactory(beanManager);
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
            final ContextHandle contextHandle = createContextHandle();
            final JobTask task = new JobTask() {
                @Override
                public int getRequiredRemainingPermits() {
                    return jobTask.getRequiredRemainingPermits();
                }

                @Override
                public void run() {
                    final Handle handle = contextHandle.setup();
                    try {
                        jobTask.run();
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

        private ContextHandle createContextHandle() {
            final ClassLoader tccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            // If the TCCL is null, use the deployments ModuleClassLoader
            final ClassLoaderContextHandle classLoaderContextHandle = (tccl == null ? new ClassLoaderContextHandle(classLoader) : new ClassLoaderContextHandle(tccl));
            // Class loader handle must be first so the TCCL is set before the other handles execute
            return new ChainedContextHandle(classLoaderContextHandle, new NamespaceContextHandle(), new SecurityContextHandle());
        }
    }

    private class BatchJobServerActivity implements ServerActivity {
        private final AtomicBoolean jobsStopped = new AtomicBoolean(false);
        private final AtomicBoolean jobsRestarted = new AtomicBoolean(false);
        private final Collection<Long> stoppedIds = Collections.synchronizedCollection(new ArrayList<>());

        @Override
        public void preSuspend(final ServerActivityCallback serverActivityCallback) {
            serverActivityCallback.done();
        }

        @Override
        public void suspended(final ServerActivityCallback serverActivityCallback) {
            try {
                stopRunningJobs(isRestartOnResume());
            } finally {
                serverActivityCallback.done();
            }
        }

        @Override
        public void resume() {
            restartStoppedJobs();
        }

        private void stopRunningJobs(final boolean queueForRestart) {
            if (jobsStopped.compareAndSet(false, true)) {
                final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
                try {
                    // Use the deployment's class loader to stop jobs
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
                    final JobOperator jobOperator = BatchRuntime.getJobOperator();
                    final Collection<String> jobNames = getJobNames(jobOperator);
                    // Look for running jobs and attempt to stop each one
                    for (String jobName : jobNames) {
                        final List<Long> runningJobs = jobOperator.getRunningExecutions(jobName);
                        for (Long id : runningJobs) {
                            try {
                                BatchLogger.LOGGER.stoppingJob(id, jobName, deploymentName);
                                jobOperator.stop(id);
                                // Queue for a restart on resume if required
                                if (queueForRestart) {
                                    stoppedIds.add(id);
                                }
                            } catch (Exception e) {
                                BatchLogger.LOGGER.stoppingJobFailed(e, id, jobName, deploymentName);
                            }
                        }
                    }
                } finally {
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
                    // Reset the stopped state
                    jobsStopped.set(false);
                }
            }
        }

        private void restartStoppedJobs() {
            if (isRestartOnResume() && jobsRestarted.compareAndSet(false, true)) {
                final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
                try {
                    // Use the deployment's class loader to stop jobs
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
                    final Collection<Long> ids = new ArrayList<>();
                    synchronized (stoppedIds) {
                        ids.addAll(stoppedIds);
                        stoppedIds.clear();
                    }
                    final JobOperator jobOperator = BatchRuntime.getJobOperator();
                    for (Long id : ids) {
                        String jobName = null;
                        try {
                            jobName = jobOperator.getJobInstance(id).getJobName();
                        } catch (Exception ignore) {
                        }
                        try {
                            final long newId = jobOperator.restart(id, RESTART_PROPS);
                            BatchLogger.LOGGER.restartingJob(jobName, id, newId);
                        } catch (Exception e) {
                            BatchLogger.LOGGER.failedRestartingJob(e, id, jobName, deploymentName);
                        }
                    }
                } finally {
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
                    // Reset the restart state
                    jobsRestarted.set(false);
                }
            }
        }

        private Collection<String> getJobNames(final JobOperator jobOperator) {
            final Set<String> knownJobNames = jobOperator.getJobNames();
            return jobXmlResolver.getJobXmlNames(classLoader).stream()
                    // Get the job name from the XML file
                    .map(xmlName -> jobXmlResolver.resolveJobName(xmlName, classLoader))
                    // Remove job names that aren't currently known to the batch runtime
                    .filter(knownJobNames::contains)
                    .collect(Collectors.toSet());
        }

        private boolean isRestartOnResume() {
            if (restartJobsOnResume == null) {
                return batchConfigurationInjector.getValue().isRestartOnResume();
            }
            return restartJobsOnResume;
        }
    }
}
