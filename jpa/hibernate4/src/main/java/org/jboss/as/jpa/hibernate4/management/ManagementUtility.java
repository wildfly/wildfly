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

package org.jboss.as.jpa.hibernate4.management;

import org.hibernate.SessionFactory;
import org.hibernate.ejb.HibernateEntityManagerFactory;
import org.hibernate.stat.Statistics;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.jpa.spi.PersistenceUnitService;
import org.jboss.as.jpa.util.JPAServiceNames;
import org.jboss.msc.service.ServiceController;

import javax.persistence.EntityManagerFactory;

/**
 * Utility class for management stuff
 *
 * @author Scott Marlow
 */
public class ManagementUtility {

    public static Statistics getStatistics(final OperationContext context, final String puname) {
        Statistics stats = null;
        final ServiceController<?> controller = context.getServiceRegistry(false).getService(JPAServiceNames.getPUServiceName(puname));
        if (controller != null) {
            // get the persistence unit service that represents the deployed persistence unit
            PersistenceUnitService persistenceUnitService = (PersistenceUnitService) controller.getValue();
            EntityManagerFactory entityManagerFactory = persistenceUnitService.getEntityManagerFactory();
            // TODO:  with JPA 2.1, if unwrap is added to EMF, change cast to "entityManagerFactory.unwrap(HibernateEntityManagerFactory.class)"
            HibernateEntityManagerFactory entityManagerFactoryImpl = (HibernateEntityManagerFactory) entityManagerFactory;
            SessionFactory sessionFactory = entityManagerFactoryImpl.getSessionFactory();
            if (sessionFactory != null) {
                stats = sessionFactory.getStatistics();
            }
        }
        return stats;
    }

}
