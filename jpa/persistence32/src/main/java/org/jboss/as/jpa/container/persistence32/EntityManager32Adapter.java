/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container.persistence32;

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.ConnectionConsumer;
import jakarta.persistence.ConnectionFunction;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FindOption;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockOption;
import jakarta.persistence.RefreshOption;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaSelect;

import jakarta.persistence.EntityManager;

/**
 * Provides shared logic to implement Jakarta Persistence 3.2 EntityManager methods that can
 * be used by {@code AbstractEntityManager} subclasses.
 */
final class EntityManager32Adapter {

    private final Adaptee adaptee;
    private final boolean isTraceEnabled;

    EntityManager32Adapter(Adaptee adaptee) {
        this.adaptee = adaptee;
        this.isTraceEnabled = adaptee.isTraceEnabled();
    }

    public <T> TypedQuery<T> createQuery(CriteriaSelect<T> selectQuery) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            // invoke underlying entity manager method and if not running in a tx
            // return a Query wrapper around the result.
            EntityManager entityManager = getEntityManager();
            return detachTypedQueryNonTxInvocation(entityManager,entityManager.createQuery(selectQuery));
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("createQuery(CriteriaSelect) took %dms", elapsed);
            }
        }
    }

    public <T> TypedQuery<T> createQuery(TypedQueryReference<T> reference) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            // invoke underlying entity manager method and if not running in a tx
            // return a Query wrapper around the result.
            EntityManager entityManager = getEntityManager();
            return detachTypedQueryNonTxInvocation(entityManager,entityManager.createQuery(reference));
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("createQuery(TypedQueryReference) took %dms", elapsed);
            }
        }
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, FindOption... options) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            final EntityManager underlyingEntityManager = getEntityManager();
            T result = getEntityManager().find(entityClass, primaryKey, options);
            detachNonTxInvocation(underlyingEntityManager);
            return result;
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("find entityClass '%s', options... took %dms", entityClass.getName(), elapsed);
            }
        }
    }

    public <T> T find(EntityGraph<T> entityGraph, Object primaryKey, FindOption... options) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            final EntityManager underlyingEntityManager = getEntityManager();
            T result = getEntityManager().find(entityGraph, primaryKey, options);
            detachNonTxInvocation(underlyingEntityManager);
            return result;
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("find entityGraph '%s' took %dms", entityGraph.getName(), elapsed);
            }
        }
    }

    public void lock(Object entity, LockModeType lockMode, LockOption... options) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            getEntityManager().lock(entity, lockMode, options);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("lock entityClass '%s', lockMode '%s', options... took %dms", getClassName(entity), getLockModeAsString(lockMode), elapsed);
            }
        }
    }

    public CacheRetrieveMode getCacheRetrieveMode() {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            return getEntityManager().getCacheRetrieveMode();
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("getCacheRetrieveMode took %dms", elapsed);
            }
        }
    }

    public CacheStoreMode getCacheStoreMode() {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            return getEntityManager().getCacheStoreMode();
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("getCacheStoreMode took %dms", elapsed);
            }
        }
    }

    public <T> T getReference(T entity) {

        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            final EntityManager underlyingEntityManager = getEntityManager();
            T result = getEntityManager().getReference(entity);
            detachNonTxInvocation(underlyingEntityManager);
            return result;
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("getReference entityClass '%s' took %dms", getClassName(entity), elapsed);
            }
        }
    }

    public void refresh(Object entity, RefreshOption... options) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            transactionIsRequired();
            getEntityManager().refresh(entity, options);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("refresh entityClass '%s', options... took %dms", getClassName(entity), elapsed);
            }
        }

    }

    public <C, T> T callWithConnection(ConnectionFunction<C, T> function) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            transactionIsRequired();
            return getEntityManager().callWithConnection(function);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("callWithConnection function '%s' took %dms", function, elapsed);
            }
        }
    }

    public <C> void runWithConnection(ConnectionConsumer<C> action) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            transactionIsRequired();
            getEntityManager().runWithConnection(action);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("runWithConnection action '%s' took %dms", action, elapsed);
            }
        }

    }

    public void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            getEntityManager().setCacheRetrieveMode(cacheRetrieveMode);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("setCacheRetrieveMode took %dms", elapsed);
            }
        }
    }

    public void setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            getEntityManager().setCacheStoreMode(cacheStoreMode);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("setCacheStoreMode took %dms", elapsed);
            }
        }
    }

    private <T> TypedQuery<T> detachTypedQueryNonTxInvocation(EntityManager entityManager, TypedQuery<T> query) {
        return adaptee.detachTypedQueryNonTxInvocation(entityManager, query);
    }

    private void detachNonTxInvocation(EntityManager underlyingEntityManager) {
        adaptee.detachNonTxInvocation(underlyingEntityManager);
    }

    private void transactionIsRequired() {
        adaptee.transactionIsRequired();
    }

    private EntityManager getEntityManager() {
        return adaptee.getEntityManager();
    }

    private String getClassName(Object entity) {
        return adaptee.getClassName(entity);
    }

    private String getLockModeAsString(LockModeType lockMode) {
        return adaptee.getLockModeAsString(lockMode);
    }

    /**
     * Methods the {@link org.jboss.as.jpa.container.AbstractEntityManager} subclass we
     * are adapting exposes to this adapter.
     */
    interface Adaptee {
        EntityManager getEntityManager();
        String getClassName(Object entity);
        String getLockModeAsString(LockModeType lockMode);
        boolean isTraceEnabled();
        <T> TypedQuery<T> detachTypedQueryNonTxInvocation(EntityManager underlyingEntityManager, TypedQuery<T> underLyingQuery);
        void detachNonTxInvocation(EntityManager underlyingEntityManager);
        void transactionIsRequired();
    }
}
