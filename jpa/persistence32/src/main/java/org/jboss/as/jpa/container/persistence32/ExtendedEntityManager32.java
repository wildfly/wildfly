/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container.persistence32;

import java.io.Serial;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.ConnectionConsumer;
import jakarta.persistence.ConnectionFunction;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FindOption;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockOption;
import jakarta.persistence.RefreshOption;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.jboss.as.jpa.container.ExtendedEntityManager;

/**
 * ExtendedEntityManager subclass that implements the EntityManager methods added in Jakarta Persistence 3.2.
 */
public final class ExtendedEntityManager32 extends ExtendedEntityManager {

    @Serial
    private static final long serialVersionUID = 6979033295500516481L;

    private transient volatile EntityManager32Adapter adapter;

    ExtendedEntityManager32(String puScopedName, EntityManager underlyingEntityManager, SynchronizationType synchronizationType, TransactionSynchronizationRegistry transactionSynchronizationRegistry, TransactionManager transactionManager) {
        super(puScopedName, underlyingEntityManager, synchronizationType, transactionSynchronizationRegistry, transactionManager);
    }

    private EntityManager32Adapter getAdapter() {
        if (adapter == null) {
            adapter = new EntityManager32Adapter(getAdaptee());
        }
        return adapter;
    }

    private EntityManager32Adapter.Adaptee getAdaptee() {
        return new EntityManager32Adapter.Adaptee() {
            @Override
            public EntityManager getEntityManager() {
                return ExtendedEntityManager32.this.getEntityManager();
            }

            @Override
            public String getClassName(Object entity) {
                return ExtendedEntityManager32.getClassName(entity);
            }

            @Override
            public String getLockModeAsString(LockModeType lockMode) {
                return ExtendedEntityManager32.getLockModeAsString(lockMode);
            }

            @Override
            public boolean isTraceEnabled() {
                return ExtendedEntityManager32.this.isTraceEnabled();
            }

            @Override
            public <T> TypedQuery<T> detachTypedQueryNonTxInvocation(EntityManager underlyingEntityManager, TypedQuery<T> underLyingQuery) {
                return ExtendedEntityManager32.this.detachTypedQueryNonTxInvocation(underlyingEntityManager, underLyingQuery);
            }

            @Override
            public void detachNonTxInvocation(EntityManager underlyingEntityManager) {
                ExtendedEntityManager32.this.detachNonTxInvocation(underlyingEntityManager);
            }

            @Override
            public void transactionIsRequired() {
                ExtendedEntityManager32.this.transactionIsRequired();
            }
        };
    }

    public <T> TypedQuery<T> createQuery(CriteriaSelect<T> selectQuery) {
        return getAdapter().createQuery(selectQuery);
    }

    public <T> TypedQuery<T> createQuery(TypedQueryReference<T> reference) {
        return getAdapter().createQuery(reference);
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, FindOption... options) {
        return getAdapter().find(entityClass, primaryKey, options);
    }

    public <T> T find(EntityGraph<T> entityGraph, Object primaryKey, FindOption... options) {
        return getAdapter().find(entityGraph, primaryKey, options);
    }

    public void lock(Object entity, LockModeType lockMode, LockOption... options) {
        getAdapter().lock(entity, lockMode, options);
    }

    public CacheRetrieveMode getCacheRetrieveMode() {
        return getAdapter().getCacheRetrieveMode();
    }

    public CacheStoreMode getCacheStoreMode() {
        return getAdapter().getCacheStoreMode();
    }

    public <T> T getReference(T entity) {
        return getAdapter().getReference(entity);
    }

    public void refresh(Object entity, RefreshOption... options) {
        getAdapter().refresh(entity, options);
    }

    public <C, T> T callWithConnection(ConnectionFunction<C, T> function) {
        return getAdapter().callWithConnection(function);
    }

    public <C> void runWithConnection(ConnectionConsumer<C> action) {
        getAdapter().runWithConnection(action);
    }

    public void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        getAdapter().setCacheRetrieveMode(cacheRetrieveMode);
    }

    public void setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        getAdapter().setCacheStoreMode(cacheStoreMode);
    }
}
