/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container.persistence32;

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
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaSelect;
import org.jboss.as.jpa.container.UnsynchronizedEntityManagerWrapper;

final class UnsynchronizedEntityManagerWrapper32 extends UnsynchronizedEntityManagerWrapper {

    private final EntityManager entityManager;

    UnsynchronizedEntityManagerWrapper32(EntityManager entityManager) {
        super(entityManager);
        this.entityManager = entityManager;
    }

    public <T> TypedQuery<T> createQuery(TypedQueryReference<T> reference) {
        return entityManager.createQuery(reference);
    }

    public <C, T> T callWithConnection(ConnectionFunction<C, T> function) {
        return entityManager.callWithConnection(function);
    }

    public <C> void runWithConnection(ConnectionConsumer<C> action) {
        entityManager.runWithConnection(action);
    }

    public void lock(Object entity, LockModeType lockMode, LockOption... options) {
        entityManager.lock(entity, lockMode, options);
    }

    public void refresh(Object entity, RefreshOption... options) {
        entityManager.refresh(entity, options);
    }

    public <T> TypedQuery<T> createQuery(CriteriaSelect<T> selectQuery) {
        return entityManager.createQuery(selectQuery);
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, FindOption... options) {
        return entityManager.find(entityClass, primaryKey, options);
    }

    public <T> T find(EntityGraph<T> entityGraph, Object primaryKey, FindOption... options) {
        return entityManager.find(entityGraph, primaryKey, options);
    }

    public CacheRetrieveMode getCacheRetrieveMode() {
        return entityManager.getCacheRetrieveMode();
    }

    public CacheStoreMode getCacheStoreMode() {
        return entityManager.getCacheStoreMode();
    }

    public void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        entityManager.setCacheRetrieveMode(cacheRetrieveMode);
    }

    public void setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        entityManager.setCacheStoreMode(cacheStoreMode);
    }

    public <T> T getReference(T entity) {
        return entityManager.getReference(entity);
    }
}
