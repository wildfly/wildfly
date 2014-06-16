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
import java.util.concurrent.Future;
import javax.transaction.TransactionManager;

import org.jberet.repository.JobRepository;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class DelegatingBatchEnvironment implements BatchEnvironment {

    private final BatchEnvironment delegate;

    public DelegatingBatchEnvironment() {
        delegate = BatchEnvironmentFactory.getInstance().getBatchEnvironment();
    }

    @Override
    public ClassLoader getClassLoader() {
        return delegate.getClassLoader();
    }

    @Override
    public ArtifactFactory getArtifactFactory() {
        return delegate.getArtifactFactory();
    }

    @Override
    public Future<?> submitTask(final Runnable task) {
        return delegate.submitTask(task);
    }

    @Override
    public <T> Future<T> submitTask(final Runnable task, final T result) {
        return delegate.submitTask(task, result);
    }

    @Override
    public <T> Future<T> submitTask(final Callable<T> task) {
        return delegate.submitTask(task);
    }

    @Override
    public TransactionManager getTransactionManager() {
        return delegate.getTransactionManager();
    }

    @Override
    public JobRepository getJobRepository() {
        return delegate.getJobRepository();
    }

    /**
     * {@inheritDoc}
     * @deprecated this is no longer used in jBeret and will be removed
     * @return
     */
    @Override
    @Deprecated
    public Properties getBatchConfigurationProperties() {
        return delegate.getBatchConfigurationProperties();
    }
}
