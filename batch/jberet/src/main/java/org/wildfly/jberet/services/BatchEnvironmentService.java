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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import javax.enterprise.inject.spi.BeanManager;
import javax.transaction.UserTransaction;

import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.jberet.BatchEnvironmentFactory;
import org.wildfly.jberet.WildFlyArtifactFactory;
import org.wildfly.jberet._private.WildFlyBatchLogger;
import org.wildfly.jberet._private.WildFlyBatchMessages;
import org.wildfly.jberet.services.ContextHandle.ChainedContextHandle;
import org.wildfly.jberet.services.ContextHandle.Handle;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchEnvironmentService implements Service<BatchEnvironment> {

    private final InjectedValue<BeanManager> beanManagerInjector = new InjectedValue<>();
    private final InjectedValue<ExecutorService> executorServiceInjector = new InjectedValue<>();
    private final InjectedValue<UserTransaction> userTransactionInjector = new InjectedValue<>();

    private BatchEnvironment batchEnvironment = null;
    private Properties properties = null;
    private ClassLoader classLoader = null;

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        WildFlyBatchLogger.LOGGER.debugf("Creating batch environment; %s", classLoader);
        final BatchEnvironment batchEnvironment = new WildFlyBatchEnvironment(classLoader,
                beanManagerInjector.getOptionalValue(), executorServiceInjector.getValue(),
                userTransactionInjector.getOptionalValue(), properties);
        // Add the service to the factory
        BatchEnvironmentFactory.getInstance().add(classLoader, batchEnvironment);
        this.batchEnvironment = batchEnvironment;
    }

    @Override
    public synchronized void stop(final StopContext context) {
        WildFlyBatchLogger.LOGGER.debugf("Removing batch environment; %s", classLoader);
        BatchEnvironmentFactory.getInstance().remove(classLoader);
        properties = null;
        classLoader = null;
        batchEnvironment = null;
    }

    @Override
    public synchronized BatchEnvironment getValue() throws IllegalStateException, IllegalArgumentException {
        return batchEnvironment;
    }

    public synchronized void setClassLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public InjectedValue<BeanManager> getBeanManagerInjector() {
        return beanManagerInjector;
    }

    public InjectedValue<ExecutorService> getExecutorServiceInjector() {
        return executorServiceInjector;
    }

    public synchronized void setProperties(final Properties properties) {
        this.properties = properties;
    }

    public InjectedValue<UserTransaction> getUserTransactionInjector() {
        return userTransactionInjector;
    }

    private static class WildFlyBatchEnvironment implements BatchEnvironment {

        private final ArtifactFactory artifactFactory;
        private final ExecutorService executorService;
        private final UserTransaction userTransaction;
        private final Properties properties;
        private final ClassLoader classLoader;

        WildFlyBatchEnvironment(final ClassLoader classLoader, final BeanManager beanManager,
                                final ExecutorService executorService, final UserTransaction userTransaction,
                                final Properties properties) {
            this.classLoader = classLoader;
            artifactFactory = (beanManager == null ? null : new WildFlyArtifactFactory(beanManager));
            this.executorService = executorService;
            this.userTransaction = userTransaction;
            this.properties = properties;
        }

        @Override
        public ClassLoader getClassLoader() {
            return classLoader;
        }

        @Override
        public ArtifactFactory getArtifactFactory() {
            if (artifactFactory == null) {
                throw WildFlyBatchMessages.MESSAGES.serviceNotInstalled("BeanManager");
            }
            return artifactFactory;
        }

        // @Override
        public ExecutorService getExecutorService() {
            return executorService;
        }

        @Override
        public Future<?> submitTask(final Runnable task) {
            final ContextHandle contextHandle = createContextHandle();
            return executorService.submit(new Runnable() {
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
        public <T> Future<T> submitTask(final Runnable task, final T result) {
            final ContextHandle contextHandle = createContextHandle();
            return executorService.submit(new Runnable() {
                @Override
                public void run() {
                    final Handle handle = contextHandle.setup();
                    try {
                        task.run();
                    } finally {
                        handle.tearDown();
                    }
                }
            }, result);
        }

        @Override
        public <T> Future<T> submitTask(final Callable<T> task) {
            final ContextHandle contextHandle = createContextHandle();
            return executorService.submit(new Callable<T>() {
                @Override
                public T call() throws Exception {
                    final Handle handle = contextHandle.setup();
                    try {
                        return task.call();
                    } finally {
                        handle.tearDown();
                    }
                }
            });
        }

        @Override
        public UserTransaction getUserTransaction() {
            if (userTransaction == null) {
                throw WildFlyBatchMessages.MESSAGES.serviceNotInstalled("UserTransaction");
            }
            return userTransaction;
        }

        @Override
        public Properties getBatchConfigurationProperties() {
            return properties;
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