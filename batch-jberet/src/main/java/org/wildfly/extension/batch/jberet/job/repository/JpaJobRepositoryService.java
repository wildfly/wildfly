/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.job.repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.JobInstance;
import jakarta.batch.runtime.StepExecution;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceProviderResolverHolder;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;
import javax.sql.DataSource;

import org.jberet.jpa.repository.JpaRepository;
import org.jberet.repository.JobRepository;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.batch.jberet._private.BatchLogger;
import org.jberet.job.model.Job;
import org.jberet.repository.ApplicationAndJobName;
import org.jberet.repository.JobExecutionSelector;
import org.jberet.runtime.AbstractStepExecution;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.jpa.util.BatchPersistenceUnitInfo;

/**
 * A service which provides a JPA job repository.
 *
 * @author a.moscatelli
 */
public class JpaJobRepositoryService extends JobRepositoryService implements Service<JobRepository> {

    private final Supplier<DataSource> dataSourceSupplier;
    private final Supplier<ExecutorService> executorSupplier;
    private EntityManagerFactory entityManagerFactory;
    private final ThreadLocal<EntityManager> entityManager = new ThreadLocal<>();
    private final ThreadLocal<JobRepository> repository = new ThreadLocal<>();

    public JpaJobRepositoryService(
            final Consumer<JobRepository> jobRepositoryConsumer,
            final Supplier<DataSource> dataSourceSupplier,
            final Supplier<ExecutorService> executorSupplier,
            final Integer executionRecordsLimit
    ) {
        super(jobRepositoryConsumer, executionRecordsLimit);
        this.dataSourceSupplier = dataSourceSupplier;
        this.executorSupplier = executorSupplier;
    }

    private EntityManager getEntityManager() {
        if (Objects.isNull(this.entityManager.get())) {
            this.entityManager.set(this.entityManagerFactory.createEntityManager());
        }
        return this.entityManager.get();
    }

    private <T> T wrapInTransaction(Supplier<T> supplier) {
        try {
            getEntityManager().getTransaction().begin();
            T result = supplier.get();
            getEntityManager().getTransaction().commit();
            return result;
        } catch (Exception e) {
            BatchLogger.LOGGER.error(e.getMessage(), e);
            getEntityManager().getTransaction().rollback();
            throw e;
        }
    }

    private void wrapInTransaction(Runnable runnable) {
        try {
            getEntityManager().getTransaction().begin();
            runnable.run();
            getEntityManager().getTransaction().commit();
        } catch (Exception e) {
            BatchLogger.LOGGER.error(e.getMessage(), e);
            getEntityManager().getTransaction().rollback();
            throw e;
        }
    }

    @Override
    public void startJobRepository(final StartContext context) throws StartException {
        final ExecutorService service = executorSupplier.get();
        final Runnable task = () -> {
            try {
                BatchPersistenceUnitInfo batchPersistenceUnitInfo = new BatchPersistenceUnitInfo();
                batchPersistenceUnitInfo.setPersistenceUnitName(JpaJobRepositoryService.class.getSimpleName());
                batchPersistenceUnitInfo.setClassLoader(Thread.currentThread().getContextClassLoader());
                batchPersistenceUnitInfo.setProperties(new Properties());
                batchPersistenceUnitInfo.setNonJtaDataSource(dataSourceSupplier.get());
                batchPersistenceUnitInfo.setTransactionType(PersistenceUnitTransactionType.RESOURCE_LOCAL);
                batchPersistenceUnitInfo.setSharedCacheMode(SharedCacheMode.ALL);
                batchPersistenceUnitInfo.setExcludeUnlistedClasses(false);
                batchPersistenceUnitInfo.setJarFileUrls(List.of(JpaRepository.class.getProtectionDomain().getCodeSource().getLocation()));
                batchPersistenceUnitInfo.setValidationMode(ValidationMode.NONE);
                Optional<PersistenceProvider> findFirst = PersistenceProviderResolverHolder.getPersistenceProviderResolver().getPersistenceProviders().stream().findFirst();
                this.entityManagerFactory = findFirst.get().createContainerEntityManagerFactory(
                        batchPersistenceUnitInfo,
                        new HashMap<>(
                                Map.of(
                                        //TO DO: When wildfly updates jakarta.persistence to 3.2 version ( currently is 3.1 ) change hardcoded string "jakarta.persistence.schema-generation.database.action" with SCHEMAGEN_DATABASE_ACTION
                                        "jakarta.persistence.schema-generation.database.action", "drop-and-create"
                                )
                        )
                );
                context.complete();
            } catch (IllegalStateException e) {
                context.failed(BatchLogger.LOGGER.failedToCreateJobRepository(e, "JPA"));
            }
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
    public void stopJobRepository(final StopContext context) {
        if (Objects.nonNull(this.entityManagerFactory) && this.entityManagerFactory.isOpen()) {
            this.entityManagerFactory.close();
        }
    }

    @Override
    protected JobRepository getDelegate() {
        if (Objects.isNull(this.repository.get())) {
            this.repository.set(
                    new JpaRepository(
                            getEntityManager()
                    )
            );
        }
        return this.repository.get();
    }

    @Override
    public void addJob(final ApplicationAndJobName applicationAndJobName, final Job job) {
        wrapInTransaction(() -> super.addJob(applicationAndJobName, job));
    }

    @Override
    public void removeJob(final String jobId) {
        wrapInTransaction(() -> super.removeJob(jobId));
    }

    @Override
    public Job getJob(final ApplicationAndJobName applicationAndJobName) {
        return wrapInTransaction(() -> super.getJob(applicationAndJobName));
    }

    @Override
    public Set<String> getJobNames() {
        return wrapInTransaction(() -> super.getJobNames());
    }

    @Override
    public boolean jobExists(final String jobName) {
        return wrapInTransaction(() -> super.jobExists(jobName));
    }

    @Override
    public JobInstanceImpl createJobInstance(final Job job, final String applicationName, final ClassLoader classLoader) {
        return wrapInTransaction(() -> super.createJobInstance(job, applicationName, classLoader));
    }

    @Override
    public void removeJobInstance(final long jobInstanceId) {
        wrapInTransaction(() -> super.removeJobInstance(jobInstanceId));
    }

    @Override
    public JobInstance getJobInstance(final long jobInstanceId) {
        return wrapInTransaction(() -> super.getJobInstance(jobInstanceId));
    }

    @Override
    public List<JobInstance> getJobInstances(final String jobName) {
        return wrapInTransaction(() -> super.getJobInstances(jobName));
    }

    @Override
    public int getJobInstanceCount(final String jobName) {
        return wrapInTransaction(() -> super.getJobInstanceCount(jobName));
    }

    @Override
    public JobExecutionImpl createJobExecution(final JobInstanceImpl jobInstance, final Properties jobParameters) {
        return wrapInTransaction(() -> super.createJobExecution(jobInstance, jobParameters));
    }

    @Override
    public JobExecution getJobExecution(final long jobExecutionId) {
        return wrapInTransaction(() -> super.getJobExecution(jobExecutionId));
    }

    @Override
    public List<JobExecution> getJobExecutions(final JobInstance jobInstance) {
        return wrapInTransaction(() -> super.getJobExecutions(jobInstance));
    }

    @Override
    public void updateJobExecution(final JobExecutionImpl jobExecution, final boolean fullUpdate, final boolean saveJobParameters) {
        wrapInTransaction(() -> super.updateJobExecution(jobExecution, fullUpdate, saveJobParameters));
    }

    @Override
    public void stopJobExecution(final JobExecutionImpl jobExecution) {
        wrapInTransaction(() -> super.stopJobExecution(jobExecution));
    }

    @Override
    public List<Long> getRunningExecutions(final String jobName) {
        return wrapInTransaction(() -> super.getRunningExecutions(jobName));
    }

    @Override
    public void removeJobExecutions(final JobExecutionSelector jobExecutionSelector) {
        wrapInTransaction(() -> super.removeJobExecutions(jobExecutionSelector));
    }

    @Override
    public List<StepExecution> getStepExecutions(final long jobExecutionId, final ClassLoader classLoader) {
        return wrapInTransaction(() -> super.getStepExecutions(jobExecutionId, classLoader));
    }

    @Override
    public StepExecutionImpl createStepExecution(final String stepName) {
        return wrapInTransaction(() -> super.createStepExecution(stepName));
    }

    @Override
    public void addStepExecution(final JobExecutionImpl jobExecution, final StepExecutionImpl stepExecution) {
        wrapInTransaction(() -> super.addStepExecution(jobExecution, stepExecution));
    }

    @Override
    public void updateStepExecution(final StepExecution stepExecution) {
        wrapInTransaction(() -> super.updateStepExecution(stepExecution));
    }

    @Override
    public StepExecutionImpl findOriginalStepExecutionForRestart(final String stepName, final JobExecutionImpl jobExecutionToRestart, final ClassLoader classLoader) {
        return wrapInTransaction(() -> super.findOriginalStepExecutionForRestart(stepName, jobExecutionToRestart, classLoader));
    }

    @Override
    public int countStepStartTimes(final String stepName, final long jobInstanceId) {
        return wrapInTransaction(() -> super.countStepStartTimes(stepName, jobInstanceId));
    }

    @Override
    public void addPartitionExecution(final StepExecutionImpl enclosingStepExecution, final PartitionExecutionImpl partitionExecution) {
        wrapInTransaction(() -> super.addPartitionExecution(enclosingStepExecution, partitionExecution));
    }

    @Override
    public List<PartitionExecutionImpl> getPartitionExecutions(final long stepExecutionId, final StepExecutionImpl stepExecution, final boolean notCompletedOnly, final ClassLoader classLoader) {
        return wrapInTransaction(() -> super.getPartitionExecutions(stepExecutionId, stepExecution, notCompletedOnly, classLoader));
    }

    @Override
    public void savePersistentData(final JobExecution jobExecution, final AbstractStepExecution stepOrPartitionExecution) {
        wrapInTransaction(() -> super.savePersistentData(jobExecution, stepOrPartitionExecution));
    }

    @Override
    public int savePersistentDataIfNotStopping(final JobExecution jobExecution, final AbstractStepExecution abstractStepExecution) {
        return wrapInTransaction(() -> super.savePersistentDataIfNotStopping(jobExecution, abstractStepExecution));
    }

    @Override
    public List<Long> getJobExecutionsByJob(final String jobName) {
        return wrapInTransaction(() -> super.getJobExecutionsByJob(jobName));
    }

    @Override
    public List<Long> getJobExecutionsByJob(String string, Integer intgr) {
        return wrapInTransaction(() -> super.getJobExecutionsByJob(string, intgr));
    }

}
