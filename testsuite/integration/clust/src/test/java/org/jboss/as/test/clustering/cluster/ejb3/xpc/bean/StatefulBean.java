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

package org.jboss.as.test.clustering.cluster.ejb3.xpc.bean;

import javax.ejb.Remove;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.sql.DataSource;

import org.jboss.ejb3.annotation.Clustered;

import java.sql.Connection;

/**
 * @author Paul Ferraro
 * @author Scott Marlow
 */
@Clustered
@javax.ejb.Stateful(name = "StatefulBean")

public class StatefulBean implements Stateful {

    @PersistenceContext(unitName = "mypc", type = PersistenceContextType.EXTENDED)
    EntityManager em;

//     @EJB
//     SecondBean secondBean;


    /**
     * Create the employee but don't commit the change to the database, instead keep it in the
     * extended persistence context.
     *
     * @param name
     * @param address
     * @param id
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void createEmployee(String name, String address, int id) {

        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);

        em.persist(emp);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Employee getEmployee(int id) {
        return em.find(Employee.class, id, LockModeType.NONE);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Employee getSecondBeanEmployee(int id) {
        //return secondBean.getEmployee(id);
        return em.find(Employee.class, id, LockModeType.NONE);
    }

    @Override
    @Remove
    public void destroy() {

    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void flush() {

    }


    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public int executeNativeSQL(String nativeSql) {
        return em.createNativeQuery(nativeSql).executeUpdate();
    }

}
