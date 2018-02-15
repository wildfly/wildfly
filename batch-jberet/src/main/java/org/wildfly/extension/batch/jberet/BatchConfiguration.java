/*
 * Copyright 2015 Red Hat, Inc.
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

package org.wildfly.extension.batch.jberet;

import org.jberet.repository.JobRepository;
import org.jberet.spi.JobExecutor;
import org.wildfly.security.auth.server.SecurityDomain;

/**
 * A configuration for the {@link org.jberet.spi.BatchEnvironment} behavior.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface BatchConfiguration {

    /**
     * Indicates whether or no batch jobs should be restarted on a resume operation if they were stopped during a
     * suspend.
     *
     * @return {@code true} to restart jobs on resume otherwise {@code false} to leave the jobs in a stopped state
     */
    boolean isRestartOnResume();

    /**
     * Returns the default job repository to use.
     *
     * @return the default job repository
     */
    JobRepository getDefaultJobRepository();

    /**
     * Returns the default job executor to use.
     *
     * @return the default job executor
     */
    JobExecutor getDefaultJobExecutor();

    /**
     * Returns the security domain if defined.
     *
     * @return the security domain or {@code null} if not defined
     */
    SecurityDomain getSecurityDomain();
}