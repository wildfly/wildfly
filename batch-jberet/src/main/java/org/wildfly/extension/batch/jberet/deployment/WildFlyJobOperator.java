/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.batch.jberet.deployment;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;
import javax.batch.operations.JobOperator;
import javax.batch.operations.NoSuchJobException;

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
