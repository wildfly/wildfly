/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container.persistence32;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.jboss.as.jpa.container.QueryNonTxInvocationDetacher;

final class QueryNonTxInvocationDetacher32 extends QueryNonTxInvocationDetacher {

    private final EntityManager underlyingEntityManager;
    private final Query underlyingQuery;

    QueryNonTxInvocationDetacher32(EntityManager underlyingEntityManager, Query underlyingQuery) {
        super(underlyingEntityManager, underlyingQuery);
        this.underlyingEntityManager = underlyingEntityManager;
        this.underlyingQuery = underlyingQuery;
    }

    @Override
    public Object getSingleResultOrNull() {
        Object result = underlyingQuery.getSingleResultOrNull();
        /**
         * The purpose of this wrapper class is so that we can detach the returned entities from this method.
         * Call EntityManager.clear will accomplish that.
         */
        underlyingEntityManager.clear();
        return result;
    }

    @Override
    public Query setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        underlyingQuery.setCacheRetrieveMode(cacheRetrieveMode);
        return this;
    }

    @Override
    public CacheRetrieveMode getCacheRetrieveMode() {
        return underlyingQuery.getCacheRetrieveMode();
    }

    @Override
    public Query setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        underlyingQuery.setCacheStoreMode(cacheStoreMode);
        return this;
    }

    @Override
    public CacheStoreMode getCacheStoreMode() {
        return underlyingQuery.getCacheStoreMode();
    }

    @Override
    public Query setTimeout(Integer timeout) {
        underlyingQuery.setTimeout(timeout);
        return this;
    }

    @Override
    public Integer getTimeout() {
        return underlyingQuery.getTimeout();
    }
}
