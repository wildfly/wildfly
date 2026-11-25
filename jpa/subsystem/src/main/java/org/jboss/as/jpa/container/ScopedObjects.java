/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container;

import jakarta.persistence.EntityManager;

/**
 * Holder for an EntityManager and StatelessSession that can be
 * associated with a EntityManagerFactory in a given transaction (or non-tx) scope.
 * <p/>
 * TODO consider instead using a different key type in NoTxEmCloser and TransactionUtil,
 * one that combines a Java type with the persistence unit name. So we'd have potentially multiple
 * map entries instead of one that has a complex object.
 */
public final class ScopedObjects {
    private EntityManager em;
    private AutoCloseable statelessSession;

    public <T extends AutoCloseable> T get(Class<T> type) {
        Object obj = type == EntityManager.class ? em : statelessSession;
        return type.cast(obj);
    }

    public <T extends AutoCloseable> void set(T obj) {
        if (obj instanceof EntityManager) {
            em = (EntityManager) obj;
        } else {
            statelessSession = obj;
        }
    }

    EntityManager getEntityManager() {
        return em;
    }

    AutoCloseable getStatelessSession() {
        return statelessSession;
    }
}
