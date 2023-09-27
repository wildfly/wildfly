/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.ormxml;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
@TransactionManagement(TransactionManagementType.CONTAINER)
public class SFSBCMT {
    @PersistenceContext(unitName = "ORMpc")
    EntityManager em;

    @Resource
    SessionContext sessionContext;

    public Employee queryEmployeeName(int id) {
        return em.find(Employee.class, id);
    }


}
