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

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

/**
 * stateful session bean with an extended persistence context
 *
 * @author Scott Marlow
 */
@Stateful
@TransactionManagement(TransactionManagementType.CONTAINER)
public class SFSBXPC {
    @PersistenceContext(unitName = "mypc", type = PersistenceContextType.EXTENDED)
        EntityManager extendedEm;

    @Resource
    SessionContext sessionContext;

    public void createEmployeeNoTx(String name, String address, int id) {

        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        extendedEm.persist(emp);
    }

    /**
     * createEmployeeNoTx is expected to be called previously and
     * this method will persist the new employee after invoking a method on SFSB1 that requires
     * yet a different TX.  The called method will look for the new Employee but shouldn't see it since the owning TX
     * hasn't been committed yet.
     *
     * @param sfsbcmt
     * @param empid
     * @return should be null but if its the Employee the new TX saw dirty data from the original TX
     */

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Employee persistAfterLookupInDifferentTX(SFSBCMT sfsbcmt, int empid) {
        return sfsbcmt.queryEmployeeNameRequireNewTX(empid);
    }

}
