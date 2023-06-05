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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import jakarta.batch.operations.JobExecutionAlreadyCompleteException;
import jakarta.batch.operations.JobExecutionIsRunningException;
import jakarta.batch.operations.JobExecutionNotMostRecentException;
import jakarta.batch.operations.JobExecutionNotRunningException;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.operations.JobRestartException;
import jakarta.batch.operations.JobSecurityException;
import jakarta.batch.operations.JobStartException;
import jakarta.batch.operations.NoSuchJobException;
import jakarta.batch.operations.NoSuchJobExecutionException;
import jakarta.batch.operations.NoSuchJobInstanceException;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.JobInstance;
import jakarta.batch.runtime.StepExecution;

import org.jberet.operations.AbstractJobOperator;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.spi.BatchEnvironment;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.server.suspend.ServerActivity;
import org.jboss.as.server.suspend.ServerActivityCallback;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.batch.jberet.BatchConfiguration;
import org.wildfly.extension.batch.jberet._private.BatchLogger;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A delegating {@linkplain jakarta.batch.operations.JobOperator job operator} to interact with the batch environment on
 * deployments.
 * <p>
 * Note that for each method the job name, or derived job name, must exist for the deployment. The allowed job names and
 * job XML descriptor are determined at deployment time.
 * </p>
 * <p>
 * This implementation does change some of the API's contracts however it's only intended to be used by management
 * resources and operations. Limits the interaction with the jobs to the scope of the deployments jobs. Any behavioral
 * change will be documented.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class JobOperatorService extends AbstractJobOperator implements WildFlyJobOperator, JobOperator, Service<JobOperator> {
    private static final Properties RESTART_PROPS = new Properties();
    private final Consumer<JobOperator> jobOperatorConsumer;
    private final Supplier<BatchConfiguration> batchConfigurationSupplier;
    private final Supplier<SecurityAwareBatchEnvironment> batchEnvironmentSupplier;
    private final Supplier<ExecutorService> executorSupplier;
    private final Supplier<SuspendController> suspendControllerSupplier;
    private final Supplier<ProcessStateNotifier> processStateSupplier;

    private volatile SecurityAwareBatchEnvironment batchEnvironment;
    private volatile ClassLoader classLoader;
    private final Boolean restartJobsOnResume;
    private final WildFlyJobXmlResolver resolver;
    private final BatchJobServerActivity serverActivity;
    private final String deploymentName;

    private final ThreadLocal<Boolean> permissionsCheckEnabled = ThreadLocal.withInitial(() -> Boolean.TRUE);

    public JobOperatorService(final Consumer<JobOperator> jobOperatorConsumer,
                              final Supplier<BatchConfiguration> batchConfigurationSupplier,
                              final Supplier<SecurityAwareBatchEnvironment> batchEnvironmentSupplier,
                              final Supplier<ExecutorService> executorSupplier,
                              final Supplier<SuspendController> suspendControllerSupplier,
                              final Supplier<ProcessStateNotifier> processStateSupplier,
                              final Boolean restartJobsOnResume, final String deploymentName, final WildFlyJobXmlResolver resolver) {
        this.jobOperatorConsumer = jobOperatorConsumer;
        this.batchConfigurationSupplier = batchConfigurationSupplier;
        this.batchEnvironmentSupplier = batchEnvironmentSupplier;
        this.executorSupplier = executorSupplier;
        this.suspendControllerSupplier = suspendControllerSupplier;
        this.processStateSupplier = processStateSupplier;
        this.restartJobsOnResume = restartJobsOnResume;
        this.deploymentName = deploymentName;
        this.resolver = resolver;
        this.serverActivity = new BatchJobServerActivity();
    }

    @Override
    public void start(final StartContext context) throws StartException {
        final BatchEnvironment batchEnvironment = this.batchEnvironment = batchEnvironmentSupplier.get();
        // Get the class loader from the environment
        classLoader = batchEnvironment.getClassLoader();
        serverActivity.initialize(processStateSupplier.get().getCurrentState(), suspendControllerSupplier.get().getState());
        processStateSupplier.get().addPropertyChangeListener(serverActivity);
        suspendControllerSupplier.get().registerActivity(serverActivity);
        jobOperatorConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext context) {
        jobOperatorConsumer.accept(null);
        // Remove the server activity
        suspendControllerSupplier.get().unRegisterActivity(serverActivity);
        processStateSupplier.get().removePropertyChangeListener(serverActivity);
        final ExecutorService service = executorSupplier.get();

        final Runnable task = () -> {
            // Should already be stopped, but just to be safe we'll make one more attempt
            serverActivity.stopRunningJobs(false);
            batchEnvironment = null;
            classLoader = null;
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
    public JobOperator getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public SecurityAwareBatchEnvironment getBatchEnvironment() {
        if (batchEnvironment == null) {
            throw BatchLogger.LOGGER.jobOperatorServiceStopped();
        }
        return batchEnvironment;
    }

    @Override
    public Set<String> getJobNames() throws JobSecurityException {
        checkState();
        // job repository does not know a job if it has not been started.
        // So we rely on the resolver to provide complete job names for the deployment.
        return resolver.getJobNames();
    }

    @Override
    public int getJobInstanceCount(final String jobName) throws NoSuchJobException, JobSecurityException {
        checkState(jobName);
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            return super.getJobInstanceCount(jobName);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    @Override
    public List<JobInstance> getJobInstances(final String jobName, final int start, final int count) throws NoSuchJobException, JobSecurityException {
        checkState(jobName);
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            return super.getJobInstances(jobName, start, count);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    @Override
    public List<Long> getRunningExecutions(final String jobName) throws NoSuchJobException, JobSecurityException {
        checkState(jobName);
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            return super.getRunningExecutions(jobName);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    @Override
    public List<Long> getJobExecutionsByJob(final String jobName) {
        checkState(jobName);
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            return super.getJobExecutionsByJob(jobName);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    @Override
    public Properties getParameters(final long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        checkState();
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            final JobInstance instance = super.getJobInstance(executionId);
            validateJob(instance.getJobName());
            return super.getParameters(executionId);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    @Override
    public long start(final String jobXMLName, final Properties jobParameters) throws JobStartException, JobSecurityException {
        checkState(null, "start");
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            final String jobXml;
            if (jobXMLName.endsWith(".xml")) {
                jobXml = jobXMLName;
            } else {
                jobXml = jobXMLName + ".xml";
            }
            if (resolver.isValidJobXmlName(jobXml)) {
                return super.start(jobXml, jobParameters, getBatchEnvironment().getCurrentUserName());
            }
            throw BatchLogger.LOGGER.couldNotFindJobXml(jobXMLName);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    @Override
    public long restart(final long executionId, final Properties restartParameters) throws JobExecutionAlreadyCompleteException, NoSuchJobExecutionException, JobExecutionNotMostRecentException, JobRestartException, JobSecurityException {
        checkState(null, "restart");
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            final JobInstance instance = super.getJobInstance(executionId);
            validateJob(instance.getJobName());
            return super.restart(executionId, restartParameters, getBatchEnvironment().getCurrentUserName());
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    @Override
    public void stop(final long executionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException, JobSecurityException {
        checkState(null, "stop");
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            final JobInstance instance = super.getJobInstance(executionId);
            validateJob(instance.getJobName());
            super.stop(executionId);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    @Override
    public void abandon(final long executionId) throws NoSuchJobExecutionException, JobExecutionIsRunningException, JobSecurityException {
        checkState(null, "abandon");
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            final JobInstance instance = super.getJobInstance(executionId);
            validateJob(instance.getJobName());
            super.abandon(executionId);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    @Override
    public JobInstance getJobInstance(final long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        checkState();
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            final JobInstance instance = super.getJobInstance(executionId);
            validateJob(instance.getJobName());
            return super.getJobInstance(executionId);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    @Override
    public List<JobExecution> getJobExecutions(final JobInstance instance) throws NoSuchJobInstanceException, JobSecurityException {
        checkState();
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            validateJob(instance == null ? null : instance.getJobName());
            return super.getJobExecutions(instance);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    @Override
    public JobExecution getJobExecution(final long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        checkState();
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            final JobInstance instance = getJobInstance(executionId);
            validateJob(instance.getJobName());
            return super.getJobExecution(executionId);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    @Override
    public List<StepExecution> getStepExecutions(final long jobExecutionId) throws NoSuchJobExecutionException, JobSecurityException {
        checkState();
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            final JobInstance instance = super.getJobInstance(jobExecutionId);
            validateJob(instance.getJobName());
            return super.getStepExecutions(jobExecutionId);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    @Override
    public Collection<String> getJobXmlNames() {
        return resolver.getJobXmlNames(classLoader);
    }

    @Override
    public Collection<String> getJobXmlNames(final String jobName) {
        return resolver.getJobXmlNames(jobName);
    }

    @Override
    public Set<String> getAllJobNames() {
        return resolver.getJobNames();
    }

    private void checkState() {
        checkState(null);
    }

    private void checkState(final String jobName) {
        checkState(jobName, "read");
    }

    private void checkState(final String jobName, final String targetName) {
        if (batchEnvironment == null || classLoader == null) {
            throw BatchLogger.LOGGER.jobOperatorServiceStopped();
        }
        checkPermission(targetName);
        if (jobName != null) {
            validateJob(jobName);
        }
    }

    private void checkPermission(final String targetName) {
        if (permissionsCheckEnabled.get()) {
            final SecurityAwareBatchEnvironment environment = getBatchEnvironment();
            final SecurityIdentity identity = environment.getIdentity();
            if (identity != null) {
                final BatchPermission permission = BatchPermission.forName(targetName);
                if (!identity.implies(permission)) {
                    throw BatchLogger.LOGGER.unauthorized(identity.getPrincipal().getName(), permission);
                }
            }
        }
    }

    private synchronized void validateJob(final String name) {
        // In JBeret 1.2.x null means all jobs, in JBeret 1.3.x+ * means all jobs if the name is null or * then ignore
        // the check
        if (name == null || "*".equals(name)) return;

        // Check that this is a valid job name
        if (!resolver.isValidJobName(name)) {
            throw BatchLogger.LOGGER.noSuchJobException(name);
        }
    }


    private class BatchJobServerActivity implements ServerActivity, PropertyChangeListener {
        private final AtomicBoolean jobsStopped = new AtomicBoolean(false);
        private final AtomicBoolean jobsRestarted = new AtomicBoolean(false);
        private final Collection<Long> stoppedIds = Collections.synchronizedCollection(new ArrayList<>());
        private boolean suspended;
        private boolean running;

        private synchronized void initialize(ControlledProcessState.State processState, SuspendController.State suspendState) {
            running = processState.isRunning();
            suspended = suspendState != SuspendController.State.RUNNING;
        }

        @Override
        public void preSuspend(final ServerActivityCallback serverActivityCallback) {
            serverActivityCallback.done();
        }

        @Override
        public void suspended(final ServerActivityCallback serverActivityCallback) {
            synchronized (this) {
                suspended = true;
            }
            try {
                stopRunningJobs(isRestartOnResume());
            } finally {
                serverActivityCallback.done();
            }
        }

        @Override
        public void resume() {
            boolean doResume;
            synchronized (this) {
                suspended = false;
                doResume = running;
            }
            if (doResume) {
                restartStoppedJobs();
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            boolean doResume;
            synchronized (this) {
                ControlledProcessState.State newState = (ControlledProcessState.State) evt.getNewValue();
                running = newState.isRunning();
                doResume = running && !suspended;
            }
            if (doResume) {
                restartStoppedJobs();
            }
        }

        private void stopRunningJobs(final boolean queueForRestart) {
            if (jobsStopped.compareAndSet(false, true)) {
                final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
                permissionsCheckEnabled.set(Boolean.FALSE);
                try {
                    // Use the deployment's class loader to stop jobs
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);

                    // getJobNames() returns both active and inactive job names, so use
                    // jobRepository.getJobNames() to get all active job names, which will
                    // be filtered in the loop below for valid job names for the current deployment.
                    final Collection<String> jobNames = getJobRepository().getJobNames();
                    // Look for running jobs and attempt to stop each one
                    for (String jobName : jobNames) {
                        if (resolver.isValidJobName(jobName)) {
                            // Casting to (Supplier<List<Long>>) is done here on purpose as a workaround for a bug in 1.8.0_45
                            final List<Long> runningJobs = allowMissingJob((Supplier<List<Long>>) () -> getRunningExecutions(jobName), Collections.emptyList());
                            for (Long id : runningJobs) {
                                try {
                                    BatchLogger.LOGGER.stoppingJob(id, jobName, deploymentName);
                                    // We want to skip the permissions check, we need to stop jobs regardless of the
                                    // permissions
                                    stop(id);
                                    // Queue for a restart on resume if required
                                    if (queueForRestart) {
                                        stoppedIds.add(id);
                                    }
                                } catch (Exception e) {
                                    BatchLogger.LOGGER.stoppingJobFailed(e, id, jobName, deploymentName);
                                }
                            }
                        }
                    }
                } finally {
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
                    // Reset the stopped state
                    jobsStopped.set(false);
                    permissionsCheckEnabled.set(Boolean.TRUE);
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
                    for (Long id : ids) {
                        String jobName = null;
                        String user = null;
                        try {
                            final JobExecutionImpl execution = getJobExecutionImpl(id);
                            jobName = execution.getJobName();
                            user = execution.getUser();
                        } catch (Exception ignore) {
                        }
                        try {
                            final long newId;
                            // If the user is not null or $local, we need to restart the job with the user specified
                            if (user == null || user.equals("$local")) {
                                newId = restart(id, RESTART_PROPS);
                            } else {
                                newId = privilegedRunAs(user, () -> restart(id, RESTART_PROPS));
                            }
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

        private <V> V privilegedRunAs(final String user, final Callable<V> callable) throws Exception {
            final SecurityDomain securityDomain = getBatchEnvironment().getSecurityDomain();
            if (securityDomain == null) {
                return callable.call();
            }
            final SecurityIdentity securityIdentity;
            if (user != null) {
                if (WildFlySecurityManager.isChecking()) {
                    securityIdentity = AccessController.doPrivileged((PrivilegedAction<SecurityIdentity>) () -> securityDomain.getAnonymousSecurityIdentity().createRunAsIdentity(user, false));
                } else {
                    securityIdentity = securityDomain.getAnonymousSecurityIdentity().createRunAsIdentity(user, false);
                }
            } else {
                securityIdentity = securityDomain.getCurrentSecurityIdentity();
            }
            return securityIdentity.runAs(callable);
        }

        private boolean isRestartOnResume() {
            if (restartJobsOnResume == null) {
                return batchConfigurationSupplier.get().isRestartOnResume();
            }
            return restartJobsOnResume;
        }
    }
}
