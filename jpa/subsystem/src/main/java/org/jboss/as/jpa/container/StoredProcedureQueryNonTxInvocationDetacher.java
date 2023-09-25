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
    public int executeUpdate() {
        return underlyingStoredProcedureQuery.executeUpdate();
    }

    @Override
    public Query setMaxResults(int maxResult) {
        return underlyingStoredProcedureQuery.setMaxResults(maxResult);
    }

    @Override
    public int getMaxResults() {
        return underlyingStoredProcedureQuery.getMaxResults();
    }

    @Override
    public Query setFirstResult(int startPosition) {
        return underlyingStoredProcedureQuery.setFirstResult(startPosition);
    }

    @Override
    public int getFirstResult() {
        return underlyingStoredProcedureQuery.getFirstResult();
    }

    @Override
    public StoredProcedureQuery setHint(String hintName, Object value) {
        return underlyingStoredProcedureQuery.setHint(hintName, value);
    }

    @Override
    public Map<String, Object> getHints() {
        return underlyingStoredProcedureQuery.getHints();
    }

    @Override
    public <T> StoredProcedureQuery setParameter(Parameter<T> param, T value) {
        return underlyingStoredProcedureQuery.setParameter(param, value);
    }

    @Override
    public StoredProcedureQuery setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
        return underlyingStoredProcedureQuery.setParameter(param, value, temporalType);
    }

    @Override
    public StoredProcedureQuery setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
        return underlyingStoredProcedureQuery.setParameter(param, value, temporalType);
    }

    @Override
    public StoredProcedureQuery setParameter(String name, Object value) {
        return underlyingStoredProcedureQuery.setParameter(name, value);
    }

    @Override
    public StoredProcedureQuery setParameter(String name, Calendar value, TemporalType temporalType) {
        return underlyingStoredProcedureQuery.setParameter(name, value, temporalType);
    }

    @Override
    public StoredProcedureQuery setParameter(String name, Date value, TemporalType temporalType) {
        return underlyingStoredProcedureQuery.setParameter(name, value, temporalType);
    }

    @Override
    public StoredProcedureQuery setParameter(int position, Object value) {
        return underlyingStoredProcedureQuery.setParameter(position, value);
    }

    @Override
    public StoredProcedureQuery setParameter(int position, Calendar value, TemporalType temporalType) {
        return underlyingStoredProcedureQuery.setParameter(position, value, temporalType);
    }

    @Override
    public StoredProcedureQuery setParameter(int position, Date value, TemporalType temporalType) {
        return underlyingStoredProcedureQuery.setParameter(position, value, temporalType);
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
        return underlyingStoredProcedureQuery.setFlushMode(flushMode);
    }

    @Override
    public FlushModeType getFlushMode() {
        return underlyingStoredProcedureQuery.getFlushMode();
    }

    @Override
    public Query setLockMode(LockModeType lockMode) {
        return underlyingStoredProcedureQuery.setLockMode(lockMode);
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
        return underlyingStoredProcedureQuery.registerStoredProcedureParameter(position, type, mode);
    }

    @Override
    public StoredProcedureQuery registerStoredProcedureParameter(String parameterName, Class type, ParameterMode mode) {
        return underlyingStoredProcedureQuery.registerStoredProcedureParameter(parameterName, type, mode);
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
