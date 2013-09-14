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

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.enterprise.inject.spi.BeanManager;
import javax.transaction.UserTransaction;

import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.jberet.WildFlyArtifactFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchEnvironmentService implements Service<BatchEnvironment> {

    private final InjectedValue<ClassLoader> classLoaderInjector = new InjectedValue<ClassLoader>();
    private final InjectedValue<BeanManager> beanManagerInjector = new InjectedValue<BeanManager>();
    private final InjectedValue<ExecutorService> executorServiceInjector = new InjectedValue<ExecutorService>();
    private final InjectedValue<Properties> propertiesInjector = new InjectedValue<Properties>();
    private final InjectedValue<UserTransaction> userTransactionInjector = new InjectedValue<UserTransaction>();

    private volatile BatchEnvironment batchEnvironment;

    @Override
    public void start(final StartContext context) throws StartException {
        batchEnvironment = new WildFlyBatchEnvironment(classLoaderInjector.getOptionalValue(),
                beanManagerInjector.getOptionalValue(), executorServiceInjector.getValue(),
                userTransactionInjector.getValue(), propertiesInjector.getValue());
    }

    @Override
    public void stop(final StopContext context) {
        batchEnvironment = null;
    }

    @Override
    public BatchEnvironment getValue() throws IllegalStateException, IllegalArgumentException {
        return batchEnvironment;
    }

    public InjectedValue<ClassLoader> getClassLoaderInjector() {
        return classLoaderInjector;
    }

    public InjectedValue<BeanManager> getBeanManagerInjector() {
        return beanManagerInjector;
    }

    public InjectedValue<ExecutorService> getExecutorServiceInjector() {
        return executorServiceInjector;
    }

    public InjectedValue<Properties> getPropertiesInjector() {
        return propertiesInjector;
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
            artifactFactory = new WildFlyArtifactFactory(beanManager, classLoader);
            this.executorService = new WrappedExecutorService(executorService, classLoader);
            this.userTransaction = userTransaction;
            this.properties = properties;
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
        public ExecutorService getExecutorService() {
            return executorService;
        }

        @Override
        public UserTransaction getUserTransaction() {
            return userTransaction;
        }

        @Override
        public Properties getBatchConfigurationProperties() {
            return properties;
        }
    }

    private static class WrappedExecutorService implements ExecutorService {
        private final ExecutorService delegate;
        private final ClassLoader classLoader;

        private WrappedExecutorService(final ExecutorService delegate, final ClassLoader classLoader) {
            this.delegate = delegate;
            this.classLoader = classLoader;
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @Override
        public <T> Future<T> submit(final Callable<T> task) {
            final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            try {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
                return delegate.submit(task);
            } finally {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
            }
        }

        @Override
        public <T> Future<T> submit(final Runnable task, final T result) {
            final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            try {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
                return delegate.submit(task, result);
            } finally {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
            }
        }

        @Override
        public Future<?> submit(final Runnable task) {
            final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            try {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
                return delegate.submit(task);
            } finally {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
            }
        }

        @Override
        public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
            final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            try {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
                return delegate.invokeAll(tasks);
            } finally {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
            }
        }

        @Override
        public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException {
            final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            try {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
                return delegate.invokeAll(tasks, timeout, unit);
            } finally {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
            }
        }

        @Override
        public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            try {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
                return delegate.invokeAny(tasks);
            } finally {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
            }
        }

        @Override
        public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            try {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
                return delegate.invokeAny(tasks, timeout, unit);
            } finally {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
            }
        }

        @Override
        public void execute(final Runnable command) {
            final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            try {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
                delegate.execute(command);
            } finally {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
            }
        }
    }
}
