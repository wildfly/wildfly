/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.jberet;

import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import javax.transaction.TransactionManager;

import org.jberet.repository.JobRepository;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
import org.wildfly.jberet._private.WildFlyBatchLogger;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchEnvironmentFactory {

    private final ConcurrentMap<ClassLoader, BatchEnvironment> environments = new ConcurrentHashMap<ClassLoader, BatchEnvironment>();

    private static final BatchEnvironment INVALID_BATCH_ENV = new BatchEnvironment() {
        @Override
        public ClassLoader getClassLoader() {
            throw WildFlyBatchLogger.LOGGER.invalidBatchEnvironment();
        }

        @Override
        public ArtifactFactory getArtifactFactory() {
            throw WildFlyBatchLogger.LOGGER.invalidBatchEnvironment();
        }

        @Override
        public Future<?> submitTask(final Runnable runnable) {
            throw WildFlyBatchLogger.LOGGER.invalidBatchEnvironment();
        }

        @Override
        public <T> Future<T> submitTask(final Runnable runnable, final T t) {
            throw WildFlyBatchLogger.LOGGER.invalidBatchEnvironment();
        }

        @Override
        public <T> Future<T> submitTask(final Callable<T> callable) {
            throw WildFlyBatchLogger.LOGGER.invalidBatchEnvironment();
        }

        @Override
        public TransactionManager getTransactionManager() {
            throw WildFlyBatchLogger.LOGGER.invalidBatchEnvironment();
        }

        @Override
        public JobRepository getJobRepository() {
            throw WildFlyBatchLogger.LOGGER.invalidBatchEnvironment();
        }

        /**
         * {@inheritDoc}
         * @deprecated this is no longer used in jBeret and will be removed
         * @return
         */
        @Override
        @Deprecated
        public Properties getBatchConfigurationProperties() {
            throw WildFlyBatchLogger.LOGGER.invalidBatchEnvironment();
        }
    };

    private static class Holder {
        static final BatchEnvironmentFactory INSTANCE = new BatchEnvironmentFactory();
    }

    public static BatchEnvironmentFactory getInstance() {
        return Holder.INSTANCE;
    }

    public BatchEnvironment getBatchEnvironment() {
        return getBatchEnvironment(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
    }

    public BatchEnvironment getBatchEnvironment(final ClassLoader cl) {
        BatchEnvironment result = environments.get(cl);
        if (result == null) {
            result = INVALID_BATCH_ENV;
        }
        return result;
    }

    public void add(final BatchEnvironment batchEnvironment) {
        add(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged(), batchEnvironment);
    }

    public void add(final ClassLoader cl, final BatchEnvironment batchEnvironment) {
        environments.putIfAbsent(cl, batchEnvironment);
    }

    public BatchEnvironment remove() {
        return remove(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
    }

    public BatchEnvironment remove(final ClassLoader cl) {
        return environments.remove(cl);
    }
}
