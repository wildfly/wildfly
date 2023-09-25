/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.persistencecontext;

import java.io.Serializable;

import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.jboss.ejb3.annotation.Cache;

/**
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
@Stateful
@Cache("distributable")
public class NonExtendedStatefuBean implements Serializable, StatefulRemote {
    private static final long serialVersionUID = 1L;

    @PersistenceContext
    EntityManager manager;

    public int doit() {
        Customer cust = new Customer();
        cust.setName("Bill");
        manager.persist(cust);
        return cust.getId();
    }

    public void find(int id) {
        if (manager.find(Customer.class, id) == null)
            throw new RuntimeException("not found");
    }

    @Remove
    @Override
    public void close() {
    }
}
