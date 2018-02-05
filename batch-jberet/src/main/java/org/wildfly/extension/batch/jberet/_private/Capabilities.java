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
     * A capability for the current batch configuration.
     */
    public static final RuntimeCapability<Void> BATCH_CONFIGURATION_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.batch.configuration", false, BatchConfiguration.class)
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
}
