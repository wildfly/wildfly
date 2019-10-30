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

package org.jboss.as.test.integration.jpa.transaction.envers;

import static org.junit.Assert.assertEquals;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

/**
 * stateful session bean
 *
 * @author Madhumita Sadhukhan
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class SFSB1 {
    @PersistenceContext(unitName = "mypc")
    EntityManager em;

    @Resource
    SessionContext sessionContext;

    // create Employee
    public Employee createEmployeeTx(String name, String address, int id) {

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
            return emp;
        } catch (Exception e) {
            throw new RuntimeException("couldn't start tx", e);

        }

    }

    public void updateEmployeeTx(String address, Employee emp) {

        emp.setAddress(address);

        UserTransaction tx1 = sessionContext.getUserTransaction();
        try {
            tx1.begin();
            //System.out.println("Inside JTA updateEmployeeTx after transaction begins:--");
            em.joinTransaction();
            em.merge(emp);
            tx1.commit();

        } catch (Exception e) {
            throw new RuntimeException("couldn't start tx", e);
        }

    }

    public void updateEmployeeTxwithRollBack(String address, Employee emp) {

        emp.setAddress(address);

        UserTransaction tx1 = sessionContext.getUserTransaction();
        try {
            tx1.begin();
            em.joinTransaction();
            em.merge(emp);
            em.getTransaction().setRollbackOnly(); // force rollback of transaction
            tx1.commit();
        } catch (Exception e) {

            throw new RuntimeException("couldn't start tx", e);
        } finally {
            String obtainedaddress = retrieveOldEmployeeVersionforRollBack(emp.getId());
            assertEquals("Red Hat Purkynova Brno", obtainedaddress);

        }

    }

    public String retrieveOldEmployeeVersion(int id) {
        AuditReader reader = AuditReaderFactory.get(em);
        Employee emp1_rev = reader.find(Employee.class, id, 1);
        return emp1_rev.getAddress();
    }

    public String retrieveOldEmployeeVersionforRollBack(int id) {
        AuditReader reader = AuditReaderFactory.get(em);
        Employee emp1_rev = reader.find(Employee.class, id, 5);
        return emp1_rev.getAddress();
    }

}
