/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet;

import java.util.Properties;
import jakarta.transaction.TransactionManager;

import org.jberet.repository.JobRepository;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.JobTask;
import org.jberet.spi.JobXmlResolver;
import org.wildfly.extension.batch.jberet._private.BatchLogger;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * An environment to act as a default batch environment for deployments. This environment throws an
 * {@link jakarta.batch.operations.BatchRuntimeException} for each method. Deployments should not end up with this
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

    @Override
    public String getApplicationName() {
        throw BatchLogger.LOGGER.noBatchEnvironmentFound(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
    }
}
