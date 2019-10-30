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

package org.jboss.as.test.integration.jpa.packaging;

import javax.ejb.Stateless;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

/**
 * stateful session bean
 *
 * @author Stuart Douglas
 */
@Stateless
public class EmployeeBean {

    @PersistenceUnit(unitName = "mainPu")
    EntityManagerFactory entityManagerFactory;

    // AS7-2275 requires each PU reference to specify a persistence unit name, if there are
    // multiple persistence unit definitions.
    // as a workaround, specified the pu name
    @PersistenceUnit(unitName = "mainPu")
    EntityManagerFactory defaultEntityManagerFactory;

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    public EntityManagerFactory getDefaultEntityManagerFactory() {
        return defaultEntityManagerFactory;
    }

    // AS7-2829 bean in ejbjar should be able to access class in persistence provider
    public Class getPersistenceProviderClass(String classname) {
        Class result = null;
        try {
            result = EmployeeBean.class.getClassLoader().loadClass(classname);
        } catch (ClassNotFoundException e) {
            return null;
        }
        return result;
    }
}
