/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.service;

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import javax.sql.DataSource;

import org.jboss.as.jpa.beanmanager.ProxyBeanManager;
import org.jboss.as.jpa.classloader.TempClassLoaderFactoryImpl;
import org.jboss.as.naming.WritableServiceBasedNamingStore;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jipijapa.plugin.spi.EntityManagerFactoryBuilder;
import org.jipijapa.plugin.spi.PersistenceProviderAdaptor;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;
import org.jipijapa.plugin.spi.TwoPhaseBootstrapCapable;
import org.wildfly.security.manager.action.GetAccessControlContextAction;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Handles the first phase of the EntityManagerFactoryBuilder for starting the Persistence Unit service.
 * The PersistenceUnitServiceImpl service handles the second phase and will not start until after PhaseOnePersistenceUnitServiceImpl starts.
 *
 * @author Scott Marlow
 */
public class PhaseOnePersistenceUnitServiceImpl implements Service<PhaseOnePersistenceUnitServiceImpl> {
    private final InjectedValue<Map> properties = new InjectedValue<>();
    private final InjectedValue<DataSource> jtaDataSource = new InjectedValue<>();
    private final InjectedValue<DataSource> nonJtaDataSource = new InjectedValue<>();
    private final InjectedValue<ExecutorService> executorInjector = new InjectedValue<>();

    private static final String CDI_BEAN_MANAGER = "javax.persistence.bean.manager";

    private final PersistenceProviderAdaptor persistenceProviderAdaptor;
    private final PersistenceUnitMetadata pu;
    private final ClassLoader classLoader;
    private final ServiceName deploymentUnitServiceName;
    private final ProxyBeanManager proxyBeanManager;
    private final Object wrapperBeanManagerLifeCycle;

    private volatile EntityManagerFactoryBuilder entityManagerFactoryBuilder;

    private volatile boolean secondPhaseStarted = false;

    public PhaseOnePersistenceUnitServiceImpl(
            final ClassLoader classLoader,
            final PersistenceUnitMetadata pu,
            final PersistenceProviderAdaptor persistenceProviderAdaptor,
            final ServiceName deploymentUnitServiceName,
            final ProxyBeanManager proxyBeanManager) {
        this.pu = pu;
        this.persistenceProviderAdaptor = persistenceProviderAdaptor;
        this.classLoader = classLoader;
        this.deploymentUnitServiceName = deploymentUnitServiceName;
        this.proxyBeanManager = proxyBeanManager;
        this.wrapperBeanManagerLifeCycle = proxyBeanManager != null ? persistenceProviderAdaptor.beanManagerLifeCycle(proxyBeanManager): null;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        final ExecutorService executor = executorInjector.getValue();
        final AccessControlContext accessControlContext =
                AccessController.doPrivileged(GetAccessControlContextAction.getInstance());

        final Runnable task = new Runnable() {
            // run async in a background thread
            @Override
            public void run() {

                PrivilegedAction<Void> privilegedAction =
                        new PrivilegedAction<Void>() {
                            // run as security privileged action
                            @Override
                            public Void run() {
                                try {
                                    ROOT_LOGGER.startingPersistenceUnitService(1, pu.getScopedPersistenceUnitName());
                                    pu.setTempClassLoaderFactory(new TempClassLoaderFactoryImpl(classLoader));
                                    pu.setJtaDataSource(jtaDataSource.getOptionalValue());
                                    pu.setNonJtaDataSource(nonJtaDataSource.getOptionalValue());

                                    if (proxyBeanManager != null) {
                                        if (wrapperBeanManagerLifeCycle != null) {
                                          // pass the wrapper object representing the bean manager life cycle object
                                          properties.getValue().put(CDI_BEAN_MANAGER, wrapperBeanManagerLifeCycle);
                                        }
                                        else {
                                          properties.getValue().put(CDI_BEAN_MANAGER, proxyBeanManager);
                                        }
                                    }

                                    WritableServiceBasedNamingStore.pushOwner(deploymentUnitServiceName);
                                    entityManagerFactoryBuilder = createContainerEntityManagerFactoryBuilder();
                                    context.complete();
                                } catch (Throwable t) {
                                    context.failed(new StartException(t));
                                } finally {
                                    pu.setTempClassLoaderFactory(null);    // release the temp classloader factory (only needed when creating the EMF)
                                    WritableServiceBasedNamingStore.popOwner();
                                }

                                return null;
                            }
                        };
                WildFlySecurityManager.doChecked(privilegedAction, accessControlContext);
            }
        };
        try {
            executor.execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            context.asynchronous();
        }
    }

    @Override
    public void stop(final StopContext context) {
        final ExecutorService executor = executorInjector.getValue();
        final AccessControlContext accessControlContext =
                AccessController.doPrivileged(GetAccessControlContextAction.getInstance());

        final Runnable task = new Runnable() {
            // run async in a background thread
            @Override
            public void run() {

                PrivilegedAction<Void> privilegedAction =
                        new PrivilegedAction<Void>() {
                            // run as security privileged action
                            @Override
                            public Void run() {

                                ROOT_LOGGER.stoppingPersistenceUnitService(1, pu.getScopedPersistenceUnitName());
                                if (entityManagerFactoryBuilder != null) {
                                    WritableServiceBasedNamingStore.pushOwner(deploymentUnitServiceName);
                                    try {
                                        if (secondPhaseStarted == false) {
                                            ROOT_LOGGER.tracef("PhaseOnePersistenceUnitServiceImpl cancelling %s " +
                                                    "which didn't start (phase 2 not reached)", pu.getScopedPersistenceUnitName());
                                            entityManagerFactoryBuilder.cancel();
                                        }
                                    } catch (Throwable t) {
                                        ROOT_LOGGER.failedToStopPUService(t, pu.getScopedPersistenceUnitName());
                                    } finally {
                                        entityManagerFactoryBuilder = null;
                                        pu.setTempClassLoaderFactory(null);
                                        WritableServiceBasedNamingStore.popOwner();
                                    }
                                }
                                properties.getValue().remove(CDI_BEAN_MANAGER);
                                context.complete();

                                return null;
                            }
                        };
                WildFlySecurityManager.doChecked(privilegedAction, accessControlContext);
            }

        };
        try {
            executor.execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            context.asynchronous();
        }
    }

    public InjectedValue<ExecutorService> getExecutorInjector() {
        return executorInjector;
    }

    @Override
    public PhaseOnePersistenceUnitServiceImpl getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /**
     * Get the entity manager factory
     *
     * @return the entity manager factory
     */

    public EntityManagerFactoryBuilder getEntityManagerFactoryBuilder() {
        return entityManagerFactoryBuilder;
    }

    public void setSecondPhaseStarted(boolean secondPhaseStarted) {
        this.secondPhaseStarted = secondPhaseStarted;
    }

    public Injector<Map> getPropertiesInjector() {
        return properties;
    }

    public Injector<DataSource> getJtaDataSourceInjector() {
        return jtaDataSource;
    }

    public Injector<DataSource> getNonJtaDataSourceInjector() {
        return nonJtaDataSource;
    }

    public ProxyBeanManager getBeanManager() {
        return proxyBeanManager;
    }

    public Object getBeanManagerLifeCycle() {
        return wrapperBeanManagerLifeCycle;
    }

    /**
     * Create EE container entity manager factory
     *
     * @return EntityManagerFactory
     */
    private EntityManagerFactoryBuilder createContainerEntityManagerFactoryBuilder() {
        persistenceProviderAdaptor.beforeCreateContainerEntityManagerFactory(pu);
        try {
            TwoPhaseBootstrapCapable twoPhaseBootstrapCapable = (TwoPhaseBootstrapCapable)persistenceProviderAdaptor;
            return twoPhaseBootstrapCapable.getBootstrap(pu, properties.getValue());
        } finally {
            try {
                persistenceProviderAdaptor.afterCreateContainerEntityManagerFactory(pu);
            } finally {
                pu.setAnnotationIndex(null);    // close reference to Annotation Index (only needed during call to createContainerEntityManagerFactory)
            }
        }
    }
}
