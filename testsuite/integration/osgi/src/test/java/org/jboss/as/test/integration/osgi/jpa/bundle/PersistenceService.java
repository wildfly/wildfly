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
package org.jboss.as.test.integration.osgi.jpa.bundle;

import java.util.concurrent.Callable;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.Assert;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @author thomas.diesler@jboss.com
 * @since 28-Sep-2012
 */
public class PersistenceService implements Callable<Boolean> {

    private final BundleContext context;

    PersistenceService(BundleContext context) {
        this.context = context;
    }

    @Override
    public Boolean call() throws Exception {
        EntityManagerFactory emf = null;
        try {
            ServiceReference sref = context.getServiceReference(EntityManagerFactory.class.getName());
            emf = (EntityManagerFactory) context.getService(sref);

            Employee emp = new Employee();
            emp.setId(100);
            emp.setAddress("Sesame Street");
            emp.setName("Kermit");

            EntityManager em = emf.createEntityManager();
            em.persist(emp);

            emp = em.find(Employee.class, 100);
            Assert.assertNotNull("Employee not null", emp);

            em.remove(emp);

        } finally {
            if (emf != null) {
                emf.close();
            }
        }
        return true;
    }
}