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
import jakarta.persistence.TemporalType;
import jakarta.persistence.TypedQuery;

/**
 * for JPA 2.0 section 3.8.6
 * used by TransactionScopedEntityManager to detach entities loaded by a query in a non-Jakarta Transactions invocation.
 * This could be a proxy but wrapper classes give faster performance.
 *
 * @author Scott Marlow
 */
public class TypedQueryNonTxInvocationDetacher<X> implements TypedQuery<X> {

    private final TypedQuery<X> underlyingQuery;
    private final EntityManager underlyingEntityManager;

    TypedQueryNonTxInvocationDetacher(EntityManager underlyingEntityManager, TypedQuery<X> underlyingQuery) {
        this.underlyingQuery = underlyingQuery;
        this.underlyingEntityManager = underlyingEntityManager;
    }

    @Override
    public List<X> getResultList() {
        List<X> result = underlyingQuery.getResultList();
        /**
         * The purpose of this wrapper class is so that we can detach the returned entities from this method.
         * Call EntityManager.clear will accomplish that.
         */
        underlyingEntityManager.clear();
        return result;
    }

    @Override
    public X getSingleResult() {
        X result = (X)underlyingQuery.getSingleResult();
        /**
         * The purpose of this wrapper class is so that we can detach the returned entities from this method.
         * Call EntityManager.clear will accomplish that.
         */
        underlyingEntityManager.clear();
        return result;
    }

    @Override
    public int executeUpdate() {
        return underlyingQuery.executeUpdate();
    }

    @Override
    public TypedQuery<X> setMaxResults(int maxResult) {
        underlyingQuery.setMaxResults(maxResult);
        return this;
    }

    @Override
    public int getMaxResults() {
        return underlyingQuery.getMaxResults();
    }

    @Override
    public TypedQuery<X> setFirstResult(int startPosition) {
        underlyingQuery.setFirstResult(startPosition);
        return this;
    }

    @Override
    public int getFirstResult() {
        return underlyingQuery.getFirstResult();
    }

    @Override
    public TypedQuery<X> setHint(String hintName, Object value) {
        underlyingQuery.setHint(hintName, value);
        return this;
    }

    @Override
    public <T> TypedQuery<X> setParameter(Parameter<T> param, T value) {
        underlyingQuery.setParameter(param, value);
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
        underlyingQuery.setParameter(param, value, temporalType);
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
        underlyingQuery.setParameter(param, value, temporalType);
        return this;
    }

    @Override
    public Map<String, Object> getHints() {
        return underlyingQuery.getHints();
    }
    @Override
    public TypedQuery<X> setParameter(String name, Object value) {
        underlyingQuery.setParameter(name, value);
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(String name, Calendar value, TemporalType temporalType) {
        underlyingQuery.setParameter(name, value, temporalType);
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(String name, Date value, TemporalType temporalType) {
        underlyingQuery.setParameter(name, value, temporalType);
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(int position, Object value) {
        underlyingQuery.setParameter(position, value);
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(int position, Calendar value, TemporalType temporalType) {
        underlyingQuery.setParameter(position, value, temporalType);
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(int position, Date value, TemporalType temporalType) {
        underlyingQuery.setParameter(position, value, temporalType);
        return this;
    }

    @Override
    public Set<Parameter<?>> getParameters() {
        return underlyingQuery.getParameters();
    }

    @Override
    public Parameter<?> getParameter(String name) {
        return underlyingQuery.getParameter(name);
    }

    @Override
    public <T> Parameter<T> getParameter(String name, Class<T> type) {
        return underlyingQuery.getParameter(name, type);
    }

    @Override
    public Parameter<?> getParameter(int position) {
        return underlyingQuery.getParameter(position);
    }

    @Override
    public <T> Parameter<T> getParameter(int position, Class<T> type) {
        return underlyingQuery.getParameter(position, type);
    }

    @Override
    public boolean isBound(Parameter<?> param) {
        return underlyingQuery.isBound(param);
    }

    @Override
    public <T> T getParameterValue(Parameter<T> param) {
        return underlyingQuery.getParameterValue(param);
    }

    @Override
    public Object getParameterValue(String name) {
        return underlyingQuery.getParameterValue(name);
    }

    @Override
    public Object getParameterValue(int position) {
        return underlyingQuery.getParameterValue(position);
    }

    @Override
    public TypedQuery<X> setFlushMode(FlushModeType flushMode) {
        underlyingQuery.setFlushMode(flushMode);
        return this;
    }

    @Override
    public FlushModeType getFlushMode() {
        return underlyingQuery.getFlushMode();
    }

    @Override
    public TypedQuery<X> setLockMode(LockModeType lockMode) {
        underlyingQuery.setLockMode(lockMode);
        return this;
    }

    @Override
    public LockModeType getLockMode() {
        return underlyingQuery.getLockMode();
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        return underlyingQuery.unwrap(cls);
    }
}
