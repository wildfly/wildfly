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
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.security.auth.server.SecurityDomain;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A default batch configuration service.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
class BatchConfigurationService implements BatchConfiguration, Service<BatchConfiguration> {

    private final Consumer<BatchConfiguration> batchConfigurationConsumer;
    private final Supplier<JobRepository> jobRepositorySupplier;
    private final Supplier<JobExecutor> jobExecutorSupplier;
    private final Supplier<SecurityDomain> securityDomainSupplier;
    private volatile boolean restartOnResume;

    BatchConfigurationService(final Consumer<BatchConfiguration> batchConfigurationConsumer,
                              final Supplier<JobRepository> jobRepositorySupplier,
                              final Supplier<JobExecutor> jobExecutorSupplier,
                              final Supplier<SecurityDomain> securityDomainSupplier) {
        this.batchConfigurationConsumer = batchConfigurationConsumer;
        this.jobRepositorySupplier = jobRepositorySupplier;
        this.jobExecutorSupplier = jobExecutorSupplier;
        this.securityDomainSupplier = securityDomainSupplier;
    }

    @Override
    public boolean isRestartOnResume() {
        return restartOnResume;
    }

    protected void setRestartOnResume(final boolean restartOnResume) {
        this.restartOnResume = restartOnResume;
    }

    @Override
    public JobRepository getDefaultJobRepository() {
        return jobRepositorySupplier.get();
    }

    @Override
    public JobExecutor getDefaultJobExecutor() {
        return jobExecutorSupplier.get();
    }

    @Override
    public SecurityDomain getSecurityDomain() {
        return securityDomainSupplier != null ? securityDomainSupplier.get() : null;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        batchConfigurationConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext context) {
        batchConfigurationConsumer.accept(null);
    }

    @Override
    public BatchConfiguration getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}