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

package org.jboss.as.test.integration.jpa.version;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class SFSB1 {
    @PersistenceContext
    EntityManager em;

    @Resource
    SessionContext sessionContext;

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
            throw new RuntimeException("createEmployee couldn't start tx", e);
        }
    }

    public Employee mutateEmployee(Employee employee) {

        employee.setName(employee.getName() + " +1");

        // load the entity with no jta transaction (entity should be detached from persistence context)
        getEmployeeNoTX(employee.getId());

        // update the entity, which will increment the version field
        UserTransaction tx1 = sessionContext.getUserTransaction();
        try {
            tx1.begin();
            em.joinTransaction();
            em.merge(employee);
            em.flush();
            tx1.commit();

            // load the entity with no jta transaction (should get fresh copy of entity with updated version field)
            return getEmployeeNoTX(employee.getId());
        } catch (Exception e) {
            throw new RuntimeException("mutateEmployee couldn't start tx", e);
        }
    }

    public Employee getEmployeeNoTX(int id) {
        return em.find(Employee.class, id);
    }
}
