/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.TemporalType;

/**
 * StoredProcedureQueryNonTxInvocationDetacher
 *
 * for JPA 2.1 (Query Execution) section 3.10.7
 * used by TransactionScopedEntityManager to clear persistence context after StoredProcedureQuery (non-Jakarta Transactions) calls.
 *
 * @author Scott Marlow
 */
public class StoredProcedureQueryNonTxInvocationDetacher implements StoredProcedureQuery {

    private final EntityManager underlyingEntityManager;
    private final StoredProcedureQuery underlyingStoredProcedureQuery;

    public StoredProcedureQueryNonTxInvocationDetacher(EntityManager underlyingEntityManager, StoredProcedureQuery underlyingStoredProcedureQuery) {
        this.underlyingEntityManager = underlyingEntityManager;
        this.underlyingStoredProcedureQuery = underlyingStoredProcedureQuery;
    }

    @Override
    public List getResultList() {
        try {
            return underlyingStoredProcedureQuery.getResultList();
        } finally {
            underlyingEntityManager.clear();
        }
    }

    @Override
    public Object getSingleResult() {
        try {
            return underlyingStoredProcedureQuery.getSingleResult();
        } finally {
            underlyingEntityManager.clear();
        }
    }

    @Override
    public Object getSingleResultOrNull() {
        return underlyingStoredProcedureQuery.getSingleResultOrNull();
    }

    @Override
    public int executeUpdate() {
        return underlyingStoredProcedureQuery.executeUpdate();
    }

    @Override
    public Query setMaxResults(int maxResult) {
        underlyingStoredProcedureQuery.setMaxResults(maxResult);
        return this;
    }

    @Override
    public int getMaxResults() {
        return underlyingStoredProcedureQuery.getMaxResults();
    }

    @Override
    public Query setFirstResult(int startPosition) {
        underlyingStoredProcedureQuery.setFirstResult(startPosition);
        return this;
    }

    @Override
    public int getFirstResult() {
        return underlyingStoredProcedureQuery.getFirstResult();
    }

    @Override
    public StoredProcedureQuery setHint(String hintName, Object value) {
        underlyingStoredProcedureQuery.setHint(hintName, value);
        return this;
    }

    @Override
    public Map<String, Object> getHints() {
        return underlyingStoredProcedureQuery.getHints();
    }

    @Override
    public <T> StoredProcedureQuery setParameter(Parameter<T> param, T value) {
        underlyingStoredProcedureQuery.setParameter(param, value);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
        underlyingStoredProcedureQuery.setParameter(param, value, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
        underlyingStoredProcedureQuery.setParameter(param, value, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(String name, Object value) {
        underlyingStoredProcedureQuery.setParameter(name, value);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(String name, Calendar value, TemporalType temporalType) {
        underlyingStoredProcedureQuery.setParameter(name, value, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(String name, Date value, TemporalType temporalType) {
        underlyingStoredProcedureQuery.setParameter(name, value, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(int position, Object value) {
        underlyingStoredProcedureQuery.setParameter(position, value);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(int position, Calendar value, TemporalType temporalType) {
        underlyingStoredProcedureQuery.setParameter(position, value, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(int position, Date value, TemporalType temporalType) {
        underlyingStoredProcedureQuery.setParameter(position, value, temporalType);
        return this;
    }

    @Override
    public Set<Parameter<?>> getParameters() {
        return underlyingStoredProcedureQuery.getParameters();
    }

    @Override
    public Parameter<?> getParameter(String name) {
        return underlyingStoredProcedureQuery.getParameter(name);
    }

    @Override
    public <T> Parameter<T> getParameter(String name, Class<T> type) {
        return underlyingStoredProcedureQuery.getParameter(name, type);
    }

    @Override
    public Parameter<?> getParameter(int position) {
        return underlyingStoredProcedureQuery.getParameter(position);
    }

    @Override
    public <T> Parameter<T> getParameter(int position, Class<T> type) {
        return underlyingStoredProcedureQuery.getParameter(position, type);
    }

    @Override
    public boolean isBound(Parameter<?> param) {
        return underlyingStoredProcedureQuery.isBound(param);
    }

    @Override
    public <T> T getParameterValue(Parameter<T> param) {
        return underlyingStoredProcedureQuery.getParameterValue(param);
    }

    @Override
    public Object getParameterValue(String name) {
        return underlyingStoredProcedureQuery.getParameterValue(name);
    }

    @Override
    public Object getParameterValue(int position) {
        return underlyingStoredProcedureQuery.getParameterValue(position);
    }

    @Override
    public StoredProcedureQuery setFlushMode(FlushModeType flushMode) {
        underlyingStoredProcedureQuery.setFlushMode(flushMode);
        return this;
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

    @Override
    public FlushModeType getFlushMode() {
        return underlyingStoredProcedureQuery.getFlushMode();
    }

    @Override
    public Query setLockMode(LockModeType lockMode) {
        underlyingStoredProcedureQuery.setLockMode(lockMode);
        return this;
    }

    @Override
    public LockModeType getLockMode() {
        return underlyingStoredProcedureQuery.getLockMode();
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        return underlyingStoredProcedureQuery.unwrap(cls);
    }

    @Override
    public StoredProcedureQuery registerStoredProcedureParameter(int position, Class type, ParameterMode mode) {
        underlyingStoredProcedureQuery.registerStoredProcedureParameter(position, type, mode);
        return this;
    }

    @Override
    public StoredProcedureQuery registerStoredProcedureParameter(String parameterName, Class type, ParameterMode mode) {
        underlyingStoredProcedureQuery.registerStoredProcedureParameter(parameterName, type, mode);
        return this;
    }

    @Override
    public Object getOutputParameterValue(int position) {
        return underlyingStoredProcedureQuery.getOutputParameterValue(position);
    }

    @Override
    public Object getOutputParameterValue(String parameterName) {
        return underlyingStoredProcedureQuery.getOutputParameterValue(parameterName);
    }

    @Override
    public boolean execute() {
        return underlyingStoredProcedureQuery.execute();
    }

    @Override
    public boolean hasMoreResults() {
        return underlyingStoredProcedureQuery.hasMoreResults();
    }

    @Override
    public int getUpdateCount() {
        return underlyingStoredProcedureQuery.getUpdateCount();
    }
}
