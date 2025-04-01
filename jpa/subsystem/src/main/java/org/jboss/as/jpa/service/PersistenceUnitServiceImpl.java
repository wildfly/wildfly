/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.service;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceProvider;
import javax.sql.DataSource;
import jakarta.validation.ValidatorFactory;

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
import org.jipijapa.plugin.spi.PersistenceProviderAdaptor;
import org.jipijapa.plugin.spi.PersistenceProviderIntegratorAdaptor;
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
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class PersistenceUnitServiceImpl implements Service<PersistenceUnitService>, PersistenceUnitService {
    private final InjectedValue<DataSource> jtaDataSource = new InjectedValue<DataSource>();
    private final InjectedValue<DataSource> nonJtaDataSource = new InjectedValue<DataSource>();
    private final InjectedValue<ExecutorService> executorInjector = new InjectedValue<ExecutorService>();
    private final InjectedValue<BeanManager> beanManagerInjector = new InjectedValue<>();

    private static final String EE_NAMESPACE = BeanManager.class.getName().startsWith("javax") ? "javax" : "jakarta";
    private static final String CDI_BEAN_MANAGER = ".persistence.bean.manager";
    private static final String VALIDATOR_FACTORY = ".persistence.validation.factory";

    private final Map properties;
    private final PersistenceProviderAdaptor persistenceProviderAdaptor;
    private final List<PersistenceProviderIntegratorAdaptor> persistenceProviderIntegratorAdaptors;
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
            final Map properties,
            final ClassLoader classLoader,
            final PersistenceUnitMetadata pu,
            final PersistenceProviderAdaptor persistenceProviderAdaptor,
            final List<PersistenceProviderIntegratorAdaptor> persistenceProviderIntegratorAdaptors,
            final PersistenceProvider persistenceProvider,
            final PersistenceUnitRegistryImpl persistenceUnitRegistry,
            final ServiceName deploymentUnitServiceName,
            final ValidatorFactory validatorFactory, SetupAction javaNamespaceSetup,
            BeanManagerAfterDeploymentValidation beanManagerAfterDeploymentValidation) {
        this.properties = properties;
        this.pu = pu;
        this.persistenceProviderAdaptor = persistenceProviderAdaptor;
        this.persistenceProviderIntegratorAdaptors = persistenceProviderIntegratorAdaptors;
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

                                    WritableServiceBasedNamingStore.pushOwner(deploymentUnitServiceName);
                                    Object wrapperBeanManagerLifeCycle=null;

                                    // as per Jakarta Persistence specification contract, always pass ValidatorFactory in via standard property before
                                    // creating container EntityManagerFactory
                                    if (validatorFactory != null) {
                                        properties.put(EE_NAMESPACE + VALIDATOR_FACTORY, validatorFactory);
                                    }

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
                                          properties.put(EE_NAMESPACE + CDI_BEAN_MANAGER, wrapperBeanManagerLifeCycle);
                                        }
                                        else {
                                          properties.put(EE_NAMESPACE + CDI_BEAN_MANAGER, proxyBeanManager);
                                        }
                                    }
                                    entityManagerFactory = createContainerEntityManagerFactory();

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
                                ROOT_LOGGER.stoppingService("Persistence Unit", pu.getScopedPersistenceUnitName());
                                ClassLoader old = Thread.currentThread().getContextClassLoader();
                                Thread.currentThread().setContextClassLoader(classLoader);
                                if(javaNamespaceSetup != null) {
                                    javaNamespaceSetup.setup(Collections.<String, Object>emptyMap());
                                }
                                try {
                                    if (entityManagerFactory != null) {
                                        // protect against race condition reported by WFLY-11563
                                        synchronized (this) {
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
                                        }
                                    }
                                } finally {
                                    Thread.currentThread().setContextClassLoader(old);
                                    if (javaNamespaceSetup != null) {
                                        javaNamespaceSetup.teardown(Collections.<String, Object>emptyMap());
                                    }
                                }
                                if (proxyBeanManager != null) {
                                    synchronized (this) {
                                        if (proxyBeanManager != null) {
                                            proxyBeanManager.setDelegate(null);
                                            proxyBeanManager = null;
                                        }
                                    }
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
                    pu.getScopedPersistenceUnitName(), properties, pu.getProperties());
            return persistenceProvider.createContainerEntityManagerFactory(pu, properties);
        } finally {
            persistenceProviderAdaptor.afterCreateContainerEntityManagerFactory(pu);
            for (PersistenceProviderIntegratorAdaptor adaptor : persistenceProviderIntegratorAdaptors) {
                adaptor.afterCreateContainerEntityManagerFactory(pu);
            }
        }
    }

}
