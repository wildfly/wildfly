/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.deployment;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.operations.NoSuchJobException;

/**
 * An extended version of a {@link JobOperator} for WildFly. Allows access to the job XML descriptors.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
interface WildFlyJobOperator extends JobOperator {

    /**
     * Returns all the job XML descriptors associated with this deployment.
     *
     * @return the job XML descriptors
     */
    Collection<String> getJobXmlNames();

    /**
     * Returns the job XML descriptors associated with a job.
     *
     * @param jobName the job name to find the XML descriptors for
     *
     * @return the set of job XML descriptors the job can be run from
     */
    Collection<String> getJobXmlNames(String jobName);

    /**
     * Returns all the jobs this operator has access to. Some of these jobs may not be found with {@link #getJobNames()}
     * as they may not exist in the {@linkplain org.jberet.repository.JobRepository job repository}.
     *
     * @return a collection of all the jobs this operator has access to
     */
    Set<String> getAllJobNames();

    /**
     * Gets job execution ids belonging to the job identified by the {@code jobName}.
     * @param jobName the job name identifying the job
     * @return job execution ids belonging to the job
     * @since 25.0.0.Beta1
     */
    List<Long> getJobExecutionsByJob(final String jobName);

    /**
     * Allows safe execution of a method catching any {@link NoSuchJobException} thrown. If the exception is thrown the
     * default value is returned, otherwise the value from the supplier is returned.
     *
     * @param supplier     the supplier for the value
     * @param defaultValue the default value if a {@link NoSuchJobException} is thrown
     * @param <T>          the return type
     *
     * @return the value from the supplier or the default value if a {@link NoSuchJobException} was thrown
     */
    default <T> T allowMissingJob(final Supplier<T> supplier, final T defaultValue) {
        try {
            return supplier.get();
        } catch (NoSuchJobException ignore) {
        }
        return defaultValue;
    }
}
