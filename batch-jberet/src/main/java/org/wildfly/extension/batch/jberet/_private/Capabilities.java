/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet._private;

import org.jberet.repository.JobRepository;
import org.jberet.spi.JobExecutor;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.wildfly.extension.batch.jberet.BatchConfiguration;

/**
 * Capabilities for the batch extension. This is not to be used outside of this extension.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class Capabilities {

    /**
     * Represents the data source capability
     */
    public static final String DATA_SOURCE_CAPABILITY = "org.wildfly.data-source";

    /**
     * Name of the capability that ensures a local provider of transactions is present.
     * Once its service is started, calls to the getInstance() methods of ContextTransactionManager,
     * ContextTransactionSynchronizationRegistry and LocalUserTransaction can be made knowing
     * that the global default TM, TSR and UT will be from that provider.
     */
    public static final String LOCAL_TRANSACTION_PROVIDER_CAPABILITY = "org.wildfly.transactions.global-default-local-provider";

    /**
     * A capability for the current batch configuration.
     */
    public static final RuntimeCapability<Void> BATCH_CONFIGURATION_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.batch.configuration", false, BatchConfiguration.class)
            // BatchConfiguration itself doesn't require a TM, but any effective use of it does (BatchEnvironment)
            .addRequirements(LOCAL_TRANSACTION_PROVIDER_CAPABILITY)
            .build();

    /**
     * A capability for thread-pools.
     */
    public static final RuntimeCapability<Void> THREAD_POOL_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.batch.thread.pool", true, JobExecutor.class)
            .build();

    /**
     * A capability for all job repositories. All job repositories should use this capability regardless of the
     * implementation of the repository.
     */
    public static final RuntimeCapability<Void> JOB_REPOSITORY_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.batch.job.repository", true, JobRepository.class)
            .build();

    /**
     * The capability name for the Elytron security domain.
     */
    public static final String SECURITY_DOMAIN_CAPABILITY = "org.wildfly.security.security-domain";

    /**
     * The capability name for the kernel SuspendController
     */
    public static final String SUSPEND_CONTROLLER_CAPABILITY = "org.wildfly.server.suspend-controller";
}
