/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container.persistence32;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.StoredProcedureQuery;
import org.jboss.as.jpa.container.StoredProcedureQueryNonTxInvocationDetacher;

public class StoredProcedureQueryNonTxInvocationDetacher32 extends StoredProcedureQueryNonTxInvocationDetacher {

    private final EntityManager underlyingEntityManager;
    private final StoredProcedureQuery underlyingStoredProcedureQuery;

    protected StoredProcedureQueryNonTxInvocationDetacher32(EntityManager underlyingEntityManager, StoredProcedureQuery underlyingStoredProcedureQuery) {
        super(underlyingEntityManager, underlyingStoredProcedureQuery);
        this.underlyingEntityManager = underlyingEntityManager;
        this.underlyingStoredProcedureQuery = underlyingStoredProcedureQuery;
    }

    @Override
    public Object getSingleResultOrNull() {
        try {
            return underlyingStoredProcedureQuery.getSingleResultOrNull();
        } finally {
            underlyingEntityManager.clear();
        }
    }

    @Override
    public StoredProcedureQuery setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        underlyingStoredProcedureQuery.setCacheRetrieveMode(cacheRetrieveMode);
        return this;
    }

    @Override
    public StoredProcedureQuery setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        underlyingStoredProcedureQuery.setCacheStoreMode(cacheStoreMode);
        return this;
    }

    @Override
    public CacheRetrieveMode getCacheRetrieveMode() {
        return underlyingStoredProcedureQuery.getCacheRetrieveMode();
    }

    @Override
    public CacheStoreMode getCacheStoreMode() {
        return underlyingStoredProcedureQuery.getCacheStoreMode();
    }

    @Override
    public StoredProcedureQuery setTimeout(Integer timeout) {
        underlyingStoredProcedureQuery.setTimeout(timeout);
        return this;
    }

    @Override
    public Integer getTimeout() {
        return underlyingStoredProcedureQuery.getTimeout();
    }
}
