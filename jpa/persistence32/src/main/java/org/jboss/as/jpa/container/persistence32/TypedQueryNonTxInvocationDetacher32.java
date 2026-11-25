/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container.persistence32;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.jboss.as.jpa.container.TypedQueryNonTxInvocationDetacher;

public class TypedQueryNonTxInvocationDetacher32<X> extends TypedQueryNonTxInvocationDetacher<X> {

    private final EntityManager underlyingEntityManager;
    private final TypedQuery<X> underlyingQuery;

    protected TypedQueryNonTxInvocationDetacher32(EntityManager underlyingEntityManager, TypedQuery<X> underlyingQuery) {
        super(underlyingEntityManager, underlyingQuery);
        this.underlyingEntityManager = underlyingEntityManager;
        this.underlyingQuery = underlyingQuery;
    }

    @Override
    public X getSingleResultOrNull() {
        X result = (X)underlyingQuery.getSingleResultOrNull();
        /*
         * The purpose of this wrapper class is so that we can detach the returned entities from this method.
         * Call EntityManager.clear will accomplish that.
         */
        underlyingEntityManager.clear();
        return result;
    }

    @Override
    public TypedQuery<X> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        underlyingQuery.setCacheRetrieveMode(cacheRetrieveMode);
        return this;
    }

    @Override
    public TypedQuery<X> setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        underlyingQuery.setCacheStoreMode(cacheStoreMode);
        return this;
    }

    @Override
    public CacheRetrieveMode getCacheRetrieveMode() {
        return underlyingQuery.getCacheRetrieveMode();
    }

    @Override
    public CacheStoreMode getCacheStoreMode() {
        return underlyingQuery.getCacheStoreMode();
    }

    @Override
    public TypedQuery<X> setTimeout(Integer timeout) {
        underlyingQuery.setTimeout(timeout);
        return this;
    }

    @Override
    public Integer getTimeout() {
        return underlyingQuery.getTimeout();
    }
}
