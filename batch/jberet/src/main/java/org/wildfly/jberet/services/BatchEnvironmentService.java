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
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.ThreadContextSetup;
import org.jboss.as.ee.naming.InjectedEENamespaceContextSelector;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.jberet.BatchEnvironmentFactory;
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

    private final InjectedEENamespaceContextSelector namespaceContextSelector;
    private volatile BatchEnvironment batchEnvironment;

    public BatchEnvironmentService(final InjectedEENamespaceContextSelector namespaceContextSelector) {
        this.namespaceContextSelector = namespaceContextSelector;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        final BatchEnvironment batchEnvironment = new WildFlyBatchEnvironment(classLoaderInjector.getOptionalValue(),
                beanManagerInjector.getOptionalValue(), executorServiceInjector.getValue(),
                userTransactionInjector.getValue(), propertiesInjector.getValue(), namespaceContextSelector);
        // Add the service to the factory
        BatchEnvironmentFactory.getInstance().add(classLoaderInjector.getValue(), batchEnvironment);
        this.batchEnvironment = batchEnvironment;
    }

    @Override
    public void stop(final StopContext context) {
        if (batchEnvironment != null)
            BatchEnvironmentFactory.getInstance().remove(batchEnvironment.getClassLoader());
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
        private final ThreadContextSetup threadContextSetup;
        private final InjectedEENamespaceContextSelector namespaceContextSelector;

        WildFlyBatchEnvironment(final ClassLoader classLoader, final BeanManager beanManager,
                                final ExecutorService executorService, final UserTransaction userTransaction,
                                final Properties properties, final InjectedEENamespaceContextSelector namespaceContextSelector) {
            this.classLoader = classLoader;
            artifactFactory = new WildFlyArtifactFactory(beanManager);
            this.executorService = executorService;
            this.userTransaction = userTransaction;
            this.properties = properties;
            this.threadContextSetup = new ClassLoaderThreadContextSetup(classLoader);
            this.namespaceContextSelector = namespaceContextSelector;
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

        @Override
        public ThreadContextSetup getThreadContextSetup() {
            return threadContextSetup;
        }

        @Override
        public <T> T lookup(final String name) throws NamingException {
            NamespaceContextSelector.pushCurrentSelector(namespaceContextSelector);
            try {
                return InitialContext.doLookup(name);
            } finally {
                NamespaceContextSelector.popCurrentSelector();
            }
        }
    }

    private static class ClassLoaderThreadContextSetup implements ThreadContextSetup {

        private final ClassLoader classLoader;

        public ClassLoaderThreadContextSetup(final ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        public TearDownHandle setup() {
            final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            return new TearDownHandle() {
                @Override
                public void tearDown() {
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
                }
            };
        }
    }
}
