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

package org.jboss.as.test.integration.jpa.transaction;

import javax.ejb.EJB;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.SynchronizationType;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
public class UnsynchronizedSFSB {
    @PersistenceContext(unitName = "unsynchronized", synchronization = SynchronizationType.UNSYNCHRONIZED)
    EntityManager em;

    @EJB
    InnerUnsynchronizedSFSB innerUnsynchronizedSFSB;    // for UNSYNCHRONIZED propagation test

    @EJB
    InnerSynchronizedSFSB innerSynchronizedSFSB;        // forces a mixed SYNCHRONIZATION/UNSYNCHRONIZED ERROR


    public Employee createAndFind(String name, String address, int id) {

        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        em.persist(emp);        // new employee will not be saved to the database
        return em.find(Employee.class, id);
    }

    public void createAndJoin(String name, String address, int id) {

        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        em.persist(emp);
        em.joinTransaction();   // new employee will be saved to database
    }

    public Employee find(int id) {
        return em.find(Employee.class, id);
    }

    public Employee createAndPropagatedFind(String name, String address, int id) {

        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        em.persist(emp);        // new employee will not be saved to the database
        return innerUnsynchronizedSFSB.find(id);
    }

    public Employee createAndPropagatedFindMixExceptionExcepted(String name, String address, int id) {

        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        em.persist(emp);        // new employee will not be saved to the database
        return innerSynchronizedSFSB.find(id);
    }

    public void createAndPropagatedJoin(String name, String address, int id) {

        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        em.persist(emp);        // new employee will not be saved to the database
        innerUnsynchronizedSFSB.joinTransaction();
    }

}
