/*
 * JBoss, Home of Professional Open Source
 * Copyright 2021, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld.services.bootstrap;

import static org.jboss.as.weld.util.ResourceInjectionUtilities.getResourceAnnotated;

import java.lang.reflect.Member;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import javax.enterprise.inject.spi.InjectionPoint;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.jpa.container.PersistenceUnitSearch;
import org.jboss.as.jpa.container.TransactionScopedEntityManager;
import org.jboss.as.jpa.processor.JpaAttachments;
import org.jboss.as.jpa.service.PersistenceUnitServiceImpl;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.util.ImmediateResourceReferenceFactory;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.jboss.weld.injection.spi.helpers.SimpleResourceReference;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.security.manager.action.GetAccessControlContextAction;
import org.wildfly.transaction.client.ContextTransactionManager;

public class WeldJpaInjectionServices implements JpaInjectionServices {

    private DeploymentUnit deploymentUnit;

    public WeldJpaInjectionServices(DeploymentUnit deploymentUnit) {
        this.deploymentUnit = deploymentUnit;
    }

    @Override
    public ResourceReferenceFactory<EntityManager> registerPersistenceContextInjectionPoint(final InjectionPoint injectionPoint) {
        //TODO: cache this stuff
        final PersistenceContext context = getResourceAnnotated(injectionPoint).getAnnotation(PersistenceContext.class);
        if (context == null) {
            throw WeldLogger.ROOT_LOGGER.annotationNotFound(PersistenceContext.class, injectionPoint.getMember());
        }
        final String scopedPuName = getScopedPUName(deploymentUnit, context.unitName(), injectionPoint.getMember());
        final ServiceName persistenceUnitServiceName = PersistenceUnitServiceImpl.getPUServiceName(scopedPuName);

        final ServiceController<?> serviceController = deploymentUnit.getServiceRegistry().getRequiredService(persistenceUnitServiceName);
        //now we have the service controller, as this method is only called at runtime the service should
        //always be up
        final PersistenceUnitServiceImpl persistenceUnitService = (PersistenceUnitServiceImpl) serviceController.getValue();
        if (persistenceUnitService.getEntityManagerFactory() != null) {
            return new EntityManagerResourceReferenceFactory(scopedPuName, persistenceUnitService.getEntityManagerFactory(), context, deploymentUnit.getAttachment(JpaAttachments.TRANSACTION_SYNCHRONIZATION_REGISTRY), ContextTransactionManager.getInstance());
        } else {
            return new LazyFactory<EntityManager>(serviceController, scopedPuName, new Callable<EntityManager>() {
                @Override
                public EntityManager call() throws Exception {
                    return new TransactionScopedEntityManager(
                            scopedPuName,
                            new HashMap<>(),
                            persistenceUnitService.getEntityManagerFactory(),
                            context.synchronization(),
                            deploymentUnit.getAttachment(JpaAttachments.TRANSACTION_SYNCHRONIZATION_REGISTRY),
                            ContextTransactionManager.getInstance());
                }
            });
        }
    }

    @Override
    public ResourceReferenceFactory<EntityManagerFactory> registerPersistenceUnitInjectionPoint(final InjectionPoint injectionPoint) {
        //TODO: cache this stuff
        final PersistenceUnit context = getResourceAnnotated(injectionPoint).getAnnotation(PersistenceUnit.class);
        if (context == null) {
            throw WeldLogger.ROOT_LOGGER.annotationNotFound(PersistenceUnit.class, injectionPoint.getMember());
        }
        final String scopedPuName = getScopedPUName(deploymentUnit, context.unitName(), injectionPoint.getMember());
        final ServiceName persistenceUnitServiceName = PersistenceUnitServiceImpl.getPUServiceName(scopedPuName);

        final ServiceController<?> serviceController = deploymentUnit.getServiceRegistry().getRequiredService(persistenceUnitServiceName);
        //now we have the service controller, as this method is only called at runtime the service should
        //always be up
        final PersistenceUnitServiceImpl persistenceUnitService = (PersistenceUnitServiceImpl) serviceController.getValue();
        if (persistenceUnitService.getEntityManagerFactory() != null) {
            return new ImmediateResourceReferenceFactory<EntityManagerFactory>(persistenceUnitService.getEntityManagerFactory());
        } else {
            return new LazyFactory<EntityManagerFactory>(serviceController, scopedPuName, new Callable<EntityManagerFactory>() {
                @Override
                public EntityManagerFactory call() throws Exception {
                    return persistenceUnitService.getEntityManagerFactory();
                }
            });
        }

    }

    @Override
    public void cleanup() {
        deploymentUnit = null;
    }

    private String getScopedPUName(final DeploymentUnit deploymentUnit, final String persistenceUnitName, Member injectionPoint) {
        PersistenceUnitMetadata scopedPu;
        scopedPu = PersistenceUnitSearch.resolvePersistenceUnitSupplier(deploymentUnit, persistenceUnitName);
        if (null == scopedPu) {
            throw WeldLogger.ROOT_LOGGER.couldNotFindPersistenceUnit(persistenceUnitName, deploymentUnit.getName(), injectionPoint);
        }
        return scopedPu.getScopedPersistenceUnitName();
    }

    private static class EntityManagerResourceReferenceFactory implements ResourceReferenceFactory<EntityManager> {
        private final String scopedPuName;
        private final EntityManagerFactory entityManagerFactory;
        private final PersistenceContext context;
        private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;
        private final TransactionManager transactionManager;

        public EntityManagerResourceReferenceFactory(String scopedPuName, EntityManagerFactory entityManagerFactory, PersistenceContext context, TransactionSynchronizationRegistry transactionSynchronizationRegistry, TransactionManager transactionManager) {
            this.scopedPuName = scopedPuName;
            this.entityManagerFactory = entityManagerFactory;
            this.context = context;
            this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
            this.transactionManager = transactionManager;
        }

        @Override
        public ResourceReference<EntityManager> createResource() {
            final TransactionScopedEntityManager result = new TransactionScopedEntityManager(scopedPuName, new HashMap<>(), entityManagerFactory, context.synchronization(), transactionSynchronizationRegistry, transactionManager);
            return new SimpleResourceReference<EntityManager>(result);
        }
    }

    private static class LazyFactory<T> implements ResourceReferenceFactory<T> {
        public static final String MSC_SERVICE_THREAD = "MSC service thread";
        public static final String INJECTION_CANNOT_BE_PERFORMED_WITHIN_MSC_SERVICE_THREAD = "injection cannot be performed from JBoss Modular Service Container (MSC) service thread";
        private final Callable<T> callable;
        private final ServiceController<?> serviceController;
        private final String scopedPuName;

        public LazyFactory(ServiceController<?> serviceController, String scopedPuName, Callable<T> callable) {
            this.callable = callable;
            this.serviceController = serviceController;
            this.scopedPuName = scopedPuName;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        boolean failed = false, removed = false;

        @Override
        public ResourceReference<T> createResource() {
            serviceController.addListener(
                    new LifecycleListener() {

                        @Override
                        public void handleEvent(final ServiceController<?> controller, final LifecycleEvent event) {
                            if (event == LifecycleEvent.UP) {
                                latch.countDown();
                                controller.removeListener(this);
                            } else if (event == LifecycleEvent.FAILED) {
                                failed = true;
                                latch.countDown();
                            } else if (event == LifecycleEvent.REMOVED) {
                                removed = true;
                                latch.countDown();
                            }
                        }
                    }
            );
            final AccessControlContext accessControlContext =
                    AccessController.doPrivileged(GetAccessControlContextAction.getInstance());

            try {
                // ensure that Injection of persistence unit doesn't cause MSC service thread to block.
                PrivilegedAction<Void> threadNameCheck =
                        new PrivilegedAction<Void>() {
                            // run as security privileged action
                            @Override
                            public Void run() {
                                assert !Thread.currentThread().getName().startsWith(MSC_SERVICE_THREAD) :
                                                        INJECTION_CANNOT_BE_PERFORMED_WITHIN_MSC_SERVICE_THREAD;
                                return null;
                            }
                        };
                WildFlySecurityManager.doChecked(threadNameCheck, accessControlContext);
                latch.await();
                if (failed) {
                    throw WeldLogger.ROOT_LOGGER.persistenceUnitFailed(scopedPuName);
                } else if(removed) {
                    throw WeldLogger.ROOT_LOGGER.persistenceUnitRemoved(scopedPuName);
                }
            } catch (InterruptedException e) {
                // Thread was interrupted, which we will preserve in case a higher level operation needs to see it.
                Thread.currentThread().interrupt();
                // rather than just returning the current EntityManagerFactory (might be null or not null),
                // fail with a runtime exception.
                throw new RuntimeException(e);
            }
            return new ResourceReference<T>() {
                T persistenceUnitTarget;

                @Override
                public T getInstance() {
                    PrivilegedAction<Void> privilegedAction =
                            new PrivilegedAction<Void>() {
                                // run as security privileged action
                                @Override
                                public Void run() {
                                    try {
                                        persistenceUnitTarget = callable.call();
                                    } catch (RuntimeException e) { // rethrow PersistenceException
                                        throw e;
                                    } catch (Exception e) {  // We shouldn't get any other Exceptions but if we do, throw then as unchecked exception
                                        throw new RuntimeException(e);
                                    }
                                    return null;
                                }
                            };
                    WildFlySecurityManager.doChecked(privilegedAction, accessControlContext);
                    return persistenceUnitTarget;
                }

                @Override
                public void release() {
                }
            };
        }
    }
}
