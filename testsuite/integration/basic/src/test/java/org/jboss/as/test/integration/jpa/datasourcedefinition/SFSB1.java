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

package org.jboss.as.test.integration.jpa.datasourcedefinition;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.UserTransaction;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class SFSB1 {
    @PersistenceContext(unitName = "mypc")
    EntityManager em;

    @Resource
    SessionContext sessionContext;

    // always throws a TransactionRequiredException
    public void createEmployeeNoTx(String name, String address, int id) {


        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);

        UserTransaction tx1 = sessionContext.getUserTransaction();
        try {
            tx1.begin();
            em.joinTransaction();
            em.persist(emp);
            tx1.commit();
        } catch (Exception e) {
            throw new RuntimeException("couldn't start tx", e);
        }

        em.flush();         // should throw TransactionRequiredException
    }

    public void createEmployee(String name, String address, int id) {


        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);

        UserTransaction tx1 = sessionContext.getUserTransaction();
        try {
            tx1.begin();
            em.joinTransaction();
            em.persist(emp);
            tx1.commit();
        } catch (Exception e) {
            throw new RuntimeException("couldn't start tx", e);
        }

    }


    public Employee getEmployeeNoTX(int id) {

        return em.find(Employee.class, id);
    }

    public String queryEmployeeNameNoTX(int id) {
        Query q = em.createQuery("SELECT e.name FROM Employee e where e.id=:id");
        q.setParameter("id", new Integer(id));
        try {
            String name = (String) q.getSingleResult();
            return name;
        } catch (NoResultException expected) {
            return "success";
        } catch (Exception unexpected) {
            return unexpected.getMessage();
        }
    }

    public Employee queryEmployeeNoTX(int id) {
        TypedQuery<Employee> q = em.createQuery("SELECT e FROM Employee e where e.id=:id", Employee.class);
        q.setParameter("id", new Integer(id));
        return q.getSingleResult();
    }

    // return true if the queried Employee is detached as required by JPA 2.0 section 3.8.6
    // For a transaction scoped persistence context non jta-tx invocation, entities returned from Query
    // must be detached.
    public boolean isQueryEmployeeDetached(int id) {
        TypedQuery<Employee> q = em.createQuery("SELECT e FROM Employee e where e.id=:id", Employee.class);
        q.setParameter("id", new Integer(id));
        Employee employee = q.getSingleResult();
        return em.contains(employee) != true;
    }
}
