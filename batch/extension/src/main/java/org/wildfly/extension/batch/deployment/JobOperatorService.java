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

package org.wildfly.extension.batch.deployment;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionIsRunningException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobOperator;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.batch._private.BatchLogger;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A delegating {@linkplain javax.batch.operations.JobOperator job operator} to interact with the batch environment on
 * deployments.
 *
 * <p>
 * Note that for each method the job name, or derived job name, must exist for the deployment. The allowed job names and
 * job XML files are determined at deployment time.
 * </p>
 *
 * <p>
 * This implementation does change some of the API's contracts however it's only intended to be used by management
 * resources and operations. Limits the interaction with the jobs to the scope of the deployments jobs. Any behavioral
 * change will be documented.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class JobOperatorService implements JobOperator, Service<JobOperator> {

    private ClassLoader classLoader;
    private JobOperator delegate;
    // Guarded by this
    private final Set<String> allowedJobNames;
    // Guarded by this
    private final Set<String> allowedJobXmlNames;

    public JobOperatorService(final ClassLoader classLoader) {
        this.classLoader = classLoader;
        allowedJobNames = new LinkedHashSet<>();
        allowedJobXmlNames = new LinkedHashSet<>();
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            delegate = BatchRuntime.getJobOperator();
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    @Override
    public synchronized void stop(final StopContext context) {
        delegate = null;
        classLoader = null;
        allowedJobXmlNames.clear();
        allowedJobNames.clear();
    }

    @Override
    public JobOperator getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public Set<String> getJobNames() throws JobSecurityException {
        checkState();
        synchronized (this) {
            return new LinkedHashSet<>(allowedJobNames);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This does not throw a {@link javax.batch.operations.NoSuchJobException} if the repository does not contain the
     * job, but the job is allowed. If this case is true then {@code 0} is returned.
     * </p>
     */
    @Override
    public int getJobInstanceCount(final String jobName) throws NoSuchJobException, JobSecurityException {
        checkState(jobName);
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            try {
                return delegate.getJobInstanceCount(jobName);
            } catch (NoSuchJobException ignore) {
            }
            return 0;
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This does not throw a {@link javax.batch.operations.NoSuchJobException} if the repository does not contain the
     * job, but the job is allowed. If this case is true an empty list is returned.
     * </p>
     */
    @Override
    public List<JobInstance> getJobInstances(final String jobName, final int start, final int count) throws NoSuchJobException, JobSecurityException {
        checkState(jobName);
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            try {
                return delegate.getJobInstances(jobName, start, count);
            } catch (NoSuchJobException ignore) {
            }
            return Collections.emptyList();
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This does not throw a {@link javax.batch.operations.NoSuchJobException} if the repository does not contain the
     * job, but the job is allowed. If this case is true an empty list is returned.
     * </p>
     */
    @Override
    public List<Long> getRunningExecutions(final String jobName) throws NoSuchJobException, JobSecurityException {
        checkState(jobName);
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            try {
                return delegate.getRunningExecutions(jobName);
            } catch (NoSuchJobException ignore) {
            }
            return Collections.emptyList();
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
            final JobInstance instance = delegate.getJobInstance(executionId);
            validateJob(instance.getJobName());
            return delegate.getParameters(executionId);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    @Override
    public long start(final String jobXMLName, final Properties jobParameters) throws JobStartException, JobSecurityException {
        checkState();
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            final String jobXml;
            if (jobXMLName.endsWith(".xml")) {
                jobXml = jobXMLName;
            } else {
                jobXml = jobXMLName + ".xml";
            }
            final boolean valid;
            synchronized (this) {
                valid = allowedJobXmlNames.contains(jobXml);
            }
            if (valid) {
                return delegate.start(jobXMLName, jobParameters);
            }
            throw BatchLogger.LOGGER.couldNotFindJobXml(jobXMLName);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    @Override
    public long restart(final long executionId, final Properties restartParameters) throws JobExecutionAlreadyCompleteException, NoSuchJobExecutionException, JobExecutionNotMostRecentException, JobRestartException, JobSecurityException {
        checkState();
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            final JobInstance instance = delegate.getJobInstance(executionId);
            validateJob(instance.getJobName());
            return delegate.restart(executionId, restartParameters);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    @Override
    public void stop(final long executionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException, JobSecurityException {
        checkState();
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            final JobInstance instance = delegate.getJobInstance(executionId);
            validateJob(instance.getJobName());
            delegate.stop(executionId);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    @Override
    public void abandon(final long executionId) throws NoSuchJobExecutionException, JobExecutionIsRunningException, JobSecurityException {
        checkState();
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            final JobInstance instance = delegate.getJobInstance(executionId);
            validateJob(instance.getJobName());
            delegate.abandon(executionId);
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
            final JobInstance instance = delegate.getJobInstance(executionId);
            validateJob(instance.getJobName());
            return delegate.getJobInstance(executionId);
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
            validateJob(instance.getJobName());
            return delegate.getJobExecutions(instance);
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
            final JobInstance instance = delegate.getJobInstance(executionId);
            validateJob(instance.getJobName());
            return delegate.getJobExecution(executionId);
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
            final JobInstance instance = delegate.getJobInstance(jobExecutionId);
            validateJob(instance.getJobName());
            return delegate.getStepExecutions(jobExecutionId);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
        }
    }

    /**
     * Adds the job XML and the job name to the allowed resources to use.
     *
     * @param jobXml  the job XML file name
     * @param jobName the job name
     */
    protected synchronized void addAllowedJob(final String jobXml, final String jobName) {
        allowedJobXmlNames.add(jobXml);
        allowedJobNames.add(jobName);
    }

    private void checkState() {
        checkState(null);
    }

    private void checkState(final String jobName) {
        if (delegate == null || classLoader == null) {
            throw BatchLogger.LOGGER.jobOperatorServiceStopped();
        }
        if (jobName != null) {
            validateJob(jobName);
        }
    }

    private synchronized void validateJob(final String name) {
        if (!allowedJobNames.contains(name)) {
            throw BatchLogger.LOGGER.noSuchJobException(name);
        }
    }
}
