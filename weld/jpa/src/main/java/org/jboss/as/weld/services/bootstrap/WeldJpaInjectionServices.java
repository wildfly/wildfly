/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
import java.util.HashMap;

import javax.enterprise.inject.spi.InjectionPoint;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.jpa.container.PersistenceUnitSearch;
import org.jboss.as.jpa.container.TransactionScopedEntityManager;
import org.jboss.as.jpa.service.PersistenceUnitServiceImpl;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.util.ImmediateResourceReferenceFactory;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.jboss.weld.injection.spi.helpers.SimpleResourceReference;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;
import org.wildfly.transaction.client.ContextTransactionManager;
import org.wildfly.transaction.client.ContextTransactionSynchronizationRegistry;

public class WeldJpaInjectionServices implements JpaInjectionServices {

    private DeploymentUnit deploymentUnit;

    public WeldJpaInjectionServices(DeploymentUnit deploymentUnit) {
        this.deploymentUnit = deploymentUnit;
    }

    @Override
    public EntityManager resolvePersistenceContext(InjectionPoint injectionPoint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityManagerFactory resolvePersistenceUnit(InjectionPoint injectionPoint) {
        throw new UnsupportedOperationException();
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
        return new EntityManagerResourceReferenceFactory(scopedPuName, persistenceUnitService.getEntityManagerFactory(), context, ContextTransactionSynchronizationRegistry.getInstance(), ContextTransactionManager.getInstance());
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

        return new ImmediateResourceReferenceFactory<EntityManagerFactory>(persistenceUnitService.getEntityManagerFactory());
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
}
