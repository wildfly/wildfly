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

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.sql.DataSource;
import javax.validation.ValidatorFactory;

import org.jboss.as.jpa.beanmanager.BeanManagerAfterDeploymentValidation;
import org.jboss.as.jpa.beanmanager.ProxyBeanManager;
import org.jboss.as.jpa.classloader.TempClassLoaderFactoryImpl;
import org.jboss.as.jpa.spi.PersistenceUnitService;
import org.jboss.as.jpa.subsystem.PersistenceUnitRegistryImpl;
import org.jboss.as.jpa.util.JPAServiceNames;
import org.jboss.as.naming.WritableServiceBasedNamingStore;
import org.jboss.as.server.deployment.SetupAction;
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
import org.wildfly.security.manager.action.GetAccessControlContextAction;
import org.wildfly.security.manager.WildFlySecurityManager;

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

/**
 * Persistence Unit service that is created for each deployed persistence unit that will be referenced by the
 * persistence context/unit injector.
 * <p/>
 * The persistence unit scoped
 *
 * @author Scott Marlow
 */
public class PersistenceUnitServiceImpl implements Service<PersistenceUnitService>, PersistenceUnitService {
    private final InjectedValue<Map> properties = new InjectedValue<Map>();
    private final InjectedValue<DataSource> jtaDataSource = new InjectedValue<DataSource>();
    private final InjectedValue<DataSource> nonJtaDataSource = new InjectedValue<DataSource>();
    private final InjectedValue<ExecutorService> executorInjector = new InjectedValue<ExecutorService>();
    private final InjectedValue<BeanManager> beanManagerInjector = new InjectedValue<>();
    private final InjectedValue<PhaseOnePersistenceUnitServiceImpl> phaseOnePersistenceUnitServiceInjectedValue = new InjectedValue<>();

    private static final String CDI_BEAN_MANAGER = "javax.persistence.bean.manager";
    private static final String VALIDATOR_FACTORY = "javax.persistence.validation.factory";

    private final PersistenceProviderAdaptor persistenceProviderAdaptor;
    private final PersistenceProvider persistenceProvider;
    private final PersistenceUnitMetadata pu;
    private final ClassLoader classLoader;
    private final PersistenceUnitRegistryImpl persistenceUnitRegistry;
    private final ServiceName deploymentUnitServiceName;
    private final ValidatorFactory validatorFactory;
    private final BeanManagerAfterDeploymentValidation beanManagerAfterDeploymentValidation;

    private volatile EntityManagerFactory entityManagerFactory;
    private volatile ProxyBeanManager proxyBeanManager;
    private final SetupAction javaNamespaceSetup;

    public PersistenceUnitServiceImpl(
            final ClassLoader classLoader,
            final PersistenceUnitMetadata pu,
            final PersistenceProviderAdaptor persistenceProviderAdaptor,
            final PersistenceProvider persistenceProvider,
            final PersistenceUnitRegistryImpl persistenceUnitRegistry,
            final ServiceName deploymentUnitServiceName,
            final ValidatorFactory validatorFactory, SetupAction javaNamespaceSetup,
            BeanManagerAfterDeploymentValidation beanManagerAfterDeploymentValidation) {
        this.pu = pu;
        this.persistenceProviderAdaptor = persistenceProviderAdaptor;
        this.persistenceProvider = persistenceProvider;
        this.classLoader = classLoader;
        this.persistenceUnitRegistry = persistenceUnitRegistry;
        this.deploymentUnitServiceName = deploymentUnitServiceName;
        this.validatorFactory = validatorFactory;
        this.javaNamespaceSetup = javaNamespaceSetup;
        this.beanManagerAfterDeploymentValidation = beanManagerAfterDeploymentValidation;
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

                                ClassLoader old = Thread.currentThread().getContextClassLoader();
                                Thread.currentThread().setContextClassLoader(classLoader);
                                if(javaNamespaceSetup != null) {
                                    javaNamespaceSetup.setup(Collections.<String, Object>emptyMap());
                                }

                                try {
                                    PhaseOnePersistenceUnitServiceImpl phaseOnePersistenceUnitService = phaseOnePersistenceUnitServiceInjectedValue.getOptionalValue();
                                    WritableServiceBasedNamingStore.pushOwner(deploymentUnitServiceName);
                                    Object wrapperBeanManagerLifeCycle=null;

                                    // as per JPA specification contract, always pass ValidatorFactory in via standard property before
                                    // creating container EntityManagerFactory
                                    if (validatorFactory != null) {
                                        properties.getValue().put(VALIDATOR_FACTORY, validatorFactory);
                                    }

                                    // handle phase 2 of 2 of bootstrapping the persistence unit
                                    if (phaseOnePersistenceUnitService != null) {
                                        ROOT_LOGGER.startingPersistenceUnitService(2, pu.getScopedPersistenceUnitName());
                                        // indicate that the second phase of bootstrapping the persistence unit has started
                                        phaseOnePersistenceUnitService.setSecondPhaseStarted(true);
                                        if (beanManagerInjector.getOptionalValue() != null) {
                                            wrapperBeanManagerLifeCycle = phaseOnePersistenceUnitService.getBeanManagerLifeCycle();
                                            // update the bean manager proxy to the actual CDI bean manager
                                            proxyBeanManager = phaseOnePersistenceUnitService.getBeanManager();
                                            proxyBeanManager.setDelegate(beanManagerInjector.getOptionalValue());
                                        }
                                        EntityManagerFactoryBuilder emfBuilder = phaseOnePersistenceUnitService.getEntityManagerFactoryBuilder();

                                        // always pass the ValidatorFactory before starting the second phase of the
                                        // persistence unit bootstrap.
                                        if (validatorFactory != null) {
                                            emfBuilder.withValidatorFactory(validatorFactory);
                                        }

                                        // get the EntityManagerFactory from the second phase of the persistence unit bootstrap
                                        entityManagerFactory = emfBuilder.build();
                                    } else {
                                        ROOT_LOGGER.startingService("Persistence Unit", pu.getScopedPersistenceUnitName());
                                        // start the persistence unit in one pass (1 of 1)
                                        pu.setTempClassLoaderFactory(new TempClassLoaderFactoryImpl(classLoader));
                                        pu.setJtaDataSource(jtaDataSource.getOptionalValue());
                                        pu.setNonJtaDataSource(nonJtaDataSource.getOptionalValue());

                                        if (beanManagerInjector.getOptionalValue() != null) {
                                            proxyBeanManager = new ProxyBeanManager();
                                            proxyBeanManager.setDelegate(beanManagerInjector.getOptionalValue());
                                            wrapperBeanManagerLifeCycle = persistenceProviderAdaptor.beanManagerLifeCycle(proxyBeanManager);
                                            if (wrapperBeanManagerLifeCycle != null) {
                                              // pass the wrapper object representing the bean manager life cycle object
                                              properties.getValue().put(CDI_BEAN_MANAGER, wrapperBeanManagerLifeCycle);
                                            }
                                            else {
                                              properties.getValue().put(CDI_BEAN_MANAGER, proxyBeanManager);
                                            }
                                        }
                                        entityManagerFactory = createContainerEntityManagerFactory();
                                    }
                                    persistenceUnitRegistry.add(getScopedPersistenceUnitName(), getValue());
                                    if(wrapperBeanManagerLifeCycle != null) {
                                        beanManagerAfterDeploymentValidation.register(persistenceProviderAdaptor, wrapperBeanManagerLifeCycle);
                                    }
                                    context.complete();
                                } catch (Throwable t) {
                                    context.failed(new StartException(t));
                                } finally {
                                    Thread.currentThread().setContextClassLoader(old);
                                    pu.setTempClassLoaderFactory(null);    // release the temp classloader factory (only needed when creating the EMF)
                                    WritableServiceBasedNamingStore.popOwner();

                                    if (javaNamespaceSetup != null) {
                                        javaNamespaceSetup.teardown(Collections.<String, Object>emptyMap());
                                    }
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

                                if (phaseOnePersistenceUnitServiceInjectedValue.getOptionalValue() != null) {
                                    ROOT_LOGGER.stoppingPersistenceUnitService(2, pu.getScopedPersistenceUnitName());
                                } else {
                                    ROOT_LOGGER.stoppingService("Persistence Unit", pu.getScopedPersistenceUnitName());
                                }
                                ClassLoader old = Thread.currentThread().getContextClassLoader();
                                Thread.currentThread().setContextClassLoader(classLoader);
                                if(javaNamespaceSetup != null) {
                                    javaNamespaceSetup.setup(Collections.<String, Object>emptyMap());
                                }
                                try {
                                    if (entityManagerFactory != null) {
                                        WritableServiceBasedNamingStore.pushOwner(deploymentUnitServiceName);
                                        try {
                                            if (entityManagerFactory.isOpen()) {
                                                entityManagerFactory.close();
                                            }
                                        } catch (Throwable t) {
                                            ROOT_LOGGER.failedToStopPUService(t, pu.getScopedPersistenceUnitName());
                                        } finally {
                                            entityManagerFactory = null;
                                            pu.setTempClassLoaderFactory(null);
                                            WritableServiceBasedNamingStore.popOwner();
                                            persistenceUnitRegistry.remove(getScopedPersistenceUnitName());
                                        }
                                    }
                                } finally {
                                    Thread.currentThread().setContextClassLoader(old);
                                    if (javaNamespaceSetup != null) {
                                        javaNamespaceSetup.teardown(Collections.<String, Object>emptyMap());
                                    }
                                }
                                if (proxyBeanManager != null) {
                                    proxyBeanManager.setDelegate(null);
                                    proxyBeanManager = null;
                                }
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
    public PersistenceUnitServiceImpl getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /**
     * Get the entity manager factory
     *
     * @return the entity manager factory
     */
    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    @Override
    public String getScopedPersistenceUnitName() {
        return pu.getScopedPersistenceUnitName();
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


    public Injector<BeanManager> getBeanManagerInjector() {
        return beanManagerInjector;
    }

    /**
     * Returns the Persistence Unit service name used for creation or lookup.
     * The service name contains the unique fully scoped persistence unit name
     *
     * @param pu persistence unit definition
     * @return
     */
    public static ServiceName getPUServiceName(PersistenceUnitMetadata pu) {
        return JPAServiceNames.getPUServiceName(pu.getScopedPersistenceUnitName());
    }

    public static ServiceName getPUServiceName(String scopedPersistenceUnitName) {
        return JPAServiceNames.getPUServiceName(scopedPersistenceUnitName);
    }

    /**
     * Create EE container entity manager factory
     *
     * @return EntityManagerFactory
     */
    private EntityManagerFactory createContainerEntityManagerFactory() {
        persistenceProviderAdaptor.beforeCreateContainerEntityManagerFactory(pu);
        try {
            ROOT_LOGGER.tracef("calling createContainerEntityManagerFactory for pu=%s with integration properties=%s, application properties=%s",
                    pu.getScopedPersistenceUnitName(), properties.getValue(), pu.getProperties());
            return persistenceProvider.createContainerEntityManagerFactory(pu, properties.getValue());
        } finally {
            try {
                persistenceProviderAdaptor.afterCreateContainerEntityManagerFactory(pu);
            } finally {
                pu.setAnnotationIndex(null);    // close reference to Annotation Index (only needed during call to createContainerEntityManagerFactory)
                //This is needed if the datasource is restarted
                //pu.setTempClassLoaderFactory(null);    // close reference to temp classloader factory (only needed during call to createEntityManagerFactory)
            }
        }
    }

    public Injector<PhaseOnePersistenceUnitServiceImpl> getPhaseOnePersistenceUnitServiceImplInjector() {
        return phaseOnePersistenceUnitServiceInjectedValue;
    }
}
