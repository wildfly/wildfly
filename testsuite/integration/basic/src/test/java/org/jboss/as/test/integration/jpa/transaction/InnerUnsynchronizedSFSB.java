/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.transaction;

import jakarta.ejb.Stateful;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.SynchronizationType;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
public class InnerUnsynchronizedSFSB {
    @PersistenceContext(unitName = "unsynchronized", synchronization = SynchronizationType.UNSYNCHRONIZED)
    EntityManager em;

    public Employee find(int id) {
        return em.find(Employee.class, id);
    }

    // join persistence context to Jakarta Transactions transaction to save pending changes to database
    public void joinTransaction() {
        em.joinTransaction();
    }
}
