/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.epcpropagation.contextduel;

import jakarta.ejb.Stateful;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;

/**
 * CMT stateful bean
 *
 * @author Scott Marlow
 */
@Stateful
public class CMTPCStatefulBean {
    @PersistenceContext(type = PersistenceContextType.TRANSACTION, unitName = "mypc")
    EntityManager em;

    public Employee getEmp(int id) {
        return em.find(Employee.class, id);
    }
}
