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

import org.jboss.as.jpa.container.PersistenceUnitSearch;
import org.jboss.as.jpa.container.TransactionScopedEntityManager;
import org.jboss.as.jpa.service.PersistenceUnitService;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.weld.injection.spi.JpaInjectionServices;

import javax.enterprise.inject.spi.InjectionPoint;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.util.HashMap;

public class WeldJpaInjectionServices implements JpaInjectionServices {

    private final DeploymentUnit deploymentUnit;
    private final ServiceRegistry serviceRegistry;

    public WeldJpaInjectionServices(DeploymentUnit deploymentUnit, ServiceRegistry serviceRegistry) {
        this.deploymentUnit = deploymentUnit;
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public EntityManager resolvePersistenceContext(InjectionPoint injectionPoint) {
        //TODO: cache this stuff
        final PersistenceContext context = injectionPoint.getAnnotated().getAnnotation(PersistenceContext.class);
        if(context == null) {
            throw new RuntimeException("Could not find @PersistenceContext annotation on " + injectionPoint.getMember());
        }
        final String scopedPuName = getScopedPUName(deploymentUnit, context.unitName());
        final ServiceName persistenceUnitServiceName = PersistenceUnitService.getPUServiceName(scopedPuName);

        final ServiceController<?> serviceController = serviceRegistry.getRequiredService(persistenceUnitServiceName);
        //now we have the service controller, as this method is only called at runtime the service should
        //always be up
        PersistenceUnitService persistenceUnitService = (PersistenceUnitService)serviceController.getValue();
        return new TransactionScopedEntityManager(scopedPuName,new HashMap<Object,Object>(), persistenceUnitService.getEntityManagerFactory());
    }

    @Override
    public EntityManagerFactory resolvePersistenceUnit(InjectionPoint injectionPoint) {
        //TODO: cache this stuff
        final PersistenceUnit context = injectionPoint.getAnnotated().getAnnotation(PersistenceUnit.class);
        if(context == null) {
            throw new RuntimeException("Could not find @PersistenceUnit annotation on " + injectionPoint.getMember());
        }
        final String scopedPuName = getScopedPUName(deploymentUnit, context.unitName());
        final ServiceName persistenceUnitServiceName = PersistenceUnitService.getPUServiceName(scopedPuName);

        final ServiceController<?> serviceController = serviceRegistry.getRequiredService(persistenceUnitServiceName);
        //now we have the service controller, as this method is only called at runtime the service should
        //always be up
        PersistenceUnitService persistenceUnitService = (PersistenceUnitService)serviceController.getValue();
        return persistenceUnitService.getEntityManagerFactory();
    }

    @Override
    public void cleanup() {
    }

    private String getScopedPUName(final DeploymentUnit deploymentUnit, String persistenceUnitName) {
        String scopedPuName;
        scopedPuName = PersistenceUnitSearch.resolvePersistenceUnitSupplier(deploymentUnit, persistenceUnitName);
        if (null == scopedPuName) {
            throw new RuntimeException("Can't find a deployment unit named " +persistenceUnitName+ " at " + deploymentUnit);
        }
        return scopedPuName;
    }
}
