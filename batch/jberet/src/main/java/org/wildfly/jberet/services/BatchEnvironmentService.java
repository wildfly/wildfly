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

package org.wildfly.jberet.services;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import javax.enterprise.inject.spi.BeanManager;
import javax.transaction.TransactionManager;

import org.jberet.repository.JobRepository;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.JobXmlResolver;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.jberet.BatchEnvironmentFactory;
import org.wildfly.jberet.WildFlyArtifactFactory;
import org.wildfly.jberet._private.WildFlyBatchLogger;
import org.wildfly.jberet.services.ContextHandle.ChainedContextHandle;
import org.wildfly.jberet.services.ContextHandle.Handle;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchEnvironmentService implements Service<BatchEnvironment> {

    // This can be removed after the getBatchConfigurationProperties() is removed from jBeret
    private static final Properties PROPS = new Properties();

    private final InjectedValue<BeanManager> beanManagerInjector = new InjectedValue<>();
    private final InjectedValue<ExecutorService> executorServiceInjector = new InjectedValue<>();
    private final InjectedValue<TransactionManager> transactionManagerInjector = new InjectedValue<>();
    private final InjectedValue<JobXmlResolver> jobXmlResolverInjector = new InjectedValue<>();

    private final JobRepository jobRepository;
    private final ClassLoader classLoader;
    private BatchEnvironment batchEnvironment = null;

    public BatchEnvironmentService(final ClassLoader classLoader, final JobRepository jobRepository) {
        this.classLoader = classLoader;
        this.jobRepository = jobRepository;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        WildFlyBatchLogger.LOGGER.debugf("Creating batch environment; %s", classLoader);
        final BatchEnvironment batchEnvironment = new WildFlyBatchEnvironment(beanManagerInjector.getOptionalValue(),
                executorServiceInjector.getValue(), transactionManagerInjector.getValue(), jobXmlResolverInjector.getValue());
        // Add the service to the factory
        BatchEnvironmentFactory.getInstance().add(classLoader, batchEnvironment);
        this.batchEnvironment = batchEnvironment;
    }

    @Override
    public synchronized void stop(final StopContext context) {
        WildFlyBatchLogger.LOGGER.debugf("Removing batch environment; %s", classLoader);
        BatchEnvironmentFactory.getInstance().remove(classLoader);
        batchEnvironment = null;
    }

    @Override
    public synchronized BatchEnvironment getValue() throws IllegalStateException, IllegalArgumentException {
        return batchEnvironment;
    }

    public InjectedValue<BeanManager> getBeanManagerInjector() {
        return beanManagerInjector;
    }

    public InjectedValue<ExecutorService> getExecutorServiceInjector() {
        return executorServiceInjector;
    }

    public InjectedValue<TransactionManager> getTransactionManagerInjector() {
        return transactionManagerInjector;
    }

    public InjectedValue<JobXmlResolver> getJobXmlResolverInjector() {
        return jobXmlResolverInjector;
    }

    private class WildFlyBatchEnvironment implements BatchEnvironment {

        private final ArtifactFactory artifactFactory;
        private final ExecutorService executorService;
        private final TransactionManager transactionManager;
        private final JobXmlResolver jobXmlResolver;

        WildFlyBatchEnvironment(final BeanManager beanManager,
                                final ExecutorService executorService, final TransactionManager transactionManager, final JobXmlResolver jobXmlResolver) {
            this.jobXmlResolver = jobXmlResolver;
            artifactFactory = new WildFlyArtifactFactory(beanManager);
            this.executorService = executorService;
            this.transactionManager = transactionManager;
        }

        @Override
        public ClassLoader getClassLoader() {
            return classLoader;
        }

        @Override
        public ArtifactFactory getArtifactFactory() {
            return artifactFactory;
        }

        @Override
        public void submitTask(final Runnable task) {
            final ContextHandle contextHandle = createContextHandle();
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    final Handle handle = contextHandle.setup();
                    try {
                        task.run();
                    } finally {
                        handle.tearDown();
                    }
                }
            });
        }

        @Override
        public TransactionManager getTransactionManager() {
            return transactionManager;
        }

        @Override
        public JobRepository getJobRepository() {
            return jobRepository;
        }

        @Override
        public JobXmlResolver getJobXmlResolver() {
            return jobXmlResolver;
        }

        /**
         * {@inheritDoc}
         * @deprecated this is no longer used in jBeret and will be removed
         * @return
         */
        @Override
        @Deprecated
        public Properties getBatchConfigurationProperties() {
            return PROPS;
        }

        private ContextHandle createContextHandle() {
            final ClassLoader tccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            // If the TCCL is null, use the deployments ModuleClassLoader
            final ClassLoaderContextHandle classLoaderContextHandle = (tccl == null ? new ClassLoaderContextHandle(classLoader) : new ClassLoaderContextHandle(tccl));
            // Class loader handle must be first so the TCCL is set before the other handles execute
            return new ChainedContextHandle(classLoaderContextHandle, new NamespaceContextHandle(), new SecurityContextHandle());
        }
    }
}