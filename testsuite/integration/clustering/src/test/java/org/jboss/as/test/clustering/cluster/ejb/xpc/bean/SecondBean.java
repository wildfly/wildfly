/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.xpc.bean;

import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;

/**
 * test bean that uses the same extended persistence context as StatefulBean and therefor should always be able to
 * retrieve the same entities that are only in the extended persistence context (purposely not persisted to the database).
 *
 * @author Scott Marlow
 */
@jakarta.ejb.Stateful
public class SecondBean {
    @PersistenceContext(unitName = "mypc", type = PersistenceContextType.EXTENDED)
    EntityManager em;

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Employee getEmployee(int id) {
        return em.find(Employee.class, id, LockModeType.NONE);
    }

}
