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

package org.wildfly.extension.batch.jberet;

import java.util.Properties;
import javax.transaction.TransactionManager;

import org.jberet.repository.JobRepository;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.JobTask;
import org.jberet.spi.JobXmlResolver;
import org.wildfly.extension.batch.jberet._private.BatchLogger;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * An environment to act as a default batch environment for deployments. This environment throws an
 * {@link javax.batch.operations.BatchRuntimeException} for each method. Deployments should not end up with this
 * environment. This is used as a fallback in cases where the {@link org.jberet.spi.JobOperatorContextSelector} cannot
 * find an appropriate environment.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class DefaultBatchEnvironment implements BatchEnvironment {

    public static final DefaultBatchEnvironment INSTANCE = new DefaultBatchEnvironment();

    private DefaultBatchEnvironment() {
    }

    @Override
    public ClassLoader getClassLoader() {
        throw BatchLogger.LOGGER.noBatchEnvironmentFound(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
    }

    @Override
    public ArtifactFactory getArtifactFactory() {
        throw BatchLogger.LOGGER.noBatchEnvironmentFound(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
    }

    @Override
    public void submitTask(final JobTask jobTask) {
        throw BatchLogger.LOGGER.noBatchEnvironmentFound(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
    }

    @Override
    public TransactionManager getTransactionManager() {
        throw BatchLogger.LOGGER.noBatchEnvironmentFound(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
    }

    @Override
    public JobRepository getJobRepository() {
        throw BatchLogger.LOGGER.noBatchEnvironmentFound(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
    }

    @Override
    public JobXmlResolver getJobXmlResolver() {
        throw BatchLogger.LOGGER.noBatchEnvironmentFound(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
    }

    @Override
    public Properties getBatchConfigurationProperties() {
        throw BatchLogger.LOGGER.noBatchEnvironmentFound(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
    }
}
