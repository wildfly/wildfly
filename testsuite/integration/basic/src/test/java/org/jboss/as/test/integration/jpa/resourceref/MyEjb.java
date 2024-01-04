/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.resourceref;

import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateful
@TransactionManagement(TransactionManagementType.CONTAINER)
public class MyEjb {

    @PersistenceContext(unitName = "mainPu")
    EntityManager em;

    public Employee queryEmployeeName(int id) {
        return em.find(Employee.class, id);
    }

}
