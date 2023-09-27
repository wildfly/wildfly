/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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