/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container;

import java.util.List;
import java.util.Map;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.ConnectionConsumer;
import jakarta.persistence.ConnectionFunction;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.FindOption;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockOption;
import jakarta.persistence.Query;
import jakarta.persistence.RefreshOption;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.metamodel.Metamodel;

/**
 * UnsynchronizedEntityManagerWrapper helps track transaction scoped persistence contexts that are SynchronizationType.UNSYNCHRONIZED,
 * for error checking.
 *
 * If an SynchronizationType.SYNCHRONIZED transaction scoped persistence context is accessed
 * while there is already an SynchronizationType.UNSYNCHRONIZED (with the same pu name + in active Jakarta Transactions TX),
 * an IllegalStateException needs to be thrown as per the JPA 2.1 spec (see 7.6.4.1 Requirements for Persistence Context Propagation).
 *
 * @author Scott Marlow
 */
public class UnsynchronizedEntityManagerWrapper implements EntityManager, SynchronizationTypeAccess {

    private final EntityManager entityManager;

    public UnsynchronizedEntityManagerWrapper(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /* SynchronizationTypeAccess methods */
    @Override
    public SynchronizationType getSynchronizationType() {
        return SynchronizationType.UNSYNCHRONIZED;
    }

    /* EntityManager methods */

    @Override
    public void clear() {
        entityManager.clear();
    }

    @Override
    public void close() {
        entityManager.close();
    }

    @Override
    public boolean contains(Object entity) {
        return entityManager.contains(entity);
    }

    @Override
    public EntityGraph<?> createEntityGraph(String graphName) {
        return entityManager.createEntityGraph(graphName);
    }

    @Override
    public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
        return entityManager.createEntityGraph(rootType);
    }

    @Override
    public Query createNamedQuery(String name) {
        return entityManager.createNamedQuery(name);
    }

    @Override
    public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        return entityManager.createNamedQuery(name, resultClass);
    }

    public <T> TypedQuery<T> createQuery(TypedQueryReference<T> reference) {
        return null;
    }

    @Override
    public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
        return entityManager.createNamedStoredProcedureQuery(name);
    }

    @Override
    public Query createNativeQuery(String sqlString) {
        return entityManager.createNativeQuery(sqlString);
    }

    @Override
    public Query createNativeQuery(String sqlString, Class resultClass) {
        return entityManager.createNativeQuery(sqlString, resultClass);
    }

    @Override
    public Query createNativeQuery(String sqlString, String resultSetMapping) {
        return entityManager.createNativeQuery(sqlString, resultSetMapping);
    }

    @Override
    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        return entityManager.createQuery(criteriaQuery);
    }

    public <T> TypedQuery<T> createQuery(CriteriaSelect<T> selectQuery) {
        return null;
    }

    @Override
    public Query createQuery(CriteriaDelete deleteQuery) {
        return entityManager.createQuery(deleteQuery);
    }

    @Override
    public Query createQuery(String qlString) {
        return entityManager.createQuery(qlString);
    }

    @Override
    public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        return entityManager.createQuery(qlString, resultClass);
    }

    @Override
    public Query createQuery(CriteriaUpdate updateQuery) {
        return entityManager.createQuery(updateQuery);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
        return entityManager.createStoredProcedureQuery(procedureName);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
        return entityManager.createStoredProcedureQuery(procedureName, resultClasses);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
        return entityManager.createStoredProcedureQuery(procedureName, resultSetMappings);
    }

    @Override
    public void detach(Object entity) {
        entityManager.detach(entity);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        return entityManager.find(entityClass, primaryKey);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
        return entityManager.find(entityClass, primaryKey, lockMode);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
        return entityManager.find(entityClass, primaryKey, lockMode, properties);
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, FindOption... options) {
        return entityManager.find(entityClass, primaryKey, options);
    }

    public <T> T find(EntityGraph<T> entityGraph, Object primaryKey, FindOption... options) {
        return entityManager.find(entityGraph, primaryKey, options);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
        return entityManager.find(entityClass, primaryKey, properties);
    }

    @Override
    public void flush() {
        entityManager.flush();
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return entityManager.getCriteriaBuilder();
    }

    @Override
    public Object getDelegate() {
        return entityManager.getDelegate();
    }

    @Override
    public EntityGraph<?> getEntityGraph(String graphName) {
        return entityManager.getEntityGraph(graphName);
    }

    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
        return entityManager.getEntityGraphs(entityClass);
    }

    public <C, T> T callWithConnection(ConnectionFunction<C, T> function) {
        return entityManager.callWithConnection(function);
    }

    public <C> void runWithConnection(ConnectionConsumer<C> action) {
        entityManager.runWithConnection(action);
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        return entityManager.getEntityManagerFactory();
    }

    @Override
    public FlushModeType getFlushMode() {
        return entityManager.getFlushMode();
    }

    @Override
    public LockModeType getLockMode(Object entity) {
        return entityManager.getLockMode(entity);
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

    @Override
    public Metamodel getMetamodel() {
        return entityManager.getMetamodel();
    }

    @Override
    public Map<String, Object> getProperties() {
        return entityManager.getProperties();
    }

    @Override
    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        return entityManager.getReference(entityClass, primaryKey);
    }

    public <T> T getReference(T entity) {
        return entityManager.getReference(entity);
    }

    @Override
    public EntityTransaction getTransaction() {
        return entityManager.getTransaction();
    }

    @Override
    public boolean isJoinedToTransaction() {
        return entityManager.isJoinedToTransaction();
    }

    @Override
    public boolean isOpen() {
        return entityManager.isOpen();
    }

    @Override
    public void joinTransaction() {
        entityManager.joinTransaction();
    }

    @Override
    public void lock(Object entity, LockModeType lockMode) {
        entityManager.lock(entity, lockMode);
    }

    @Override
    public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        entityManager.lock(entity, lockMode, properties);
    }

    public void lock(Object entity, LockModeType lockMode, LockOption... options) {
        entityManager.lock(entity, lockMode, options);
    }

    @Override
    public <T> T merge(T entity) {
        return entityManager.merge(entity);
    }

    @Override
    public void persist(Object entity) {
        entityManager.persist(entity);
    }

    @Override
    public void refresh(Object entity) {
        entityManager.refresh(entity);
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode) {
        entityManager.refresh(entity, lockMode);
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        entityManager.refresh(entity, lockMode, properties);
    }

    public void refresh(Object entity, RefreshOption... options) {
        entityManager.refresh(entity, options);
    }

    @Override
    public void refresh(Object entity, Map<String, Object> properties) {
        entityManager.refresh(entity, properties);
    }

    @Override
    public void remove(Object entity) {
        entityManager.remove(entity);
    }

    @Override
    public void setFlushMode(FlushModeType flushMode) {
        entityManager.setFlushMode(flushMode);
    }

    @Override
    public void setProperty(String propertyName, Object value) {
        entityManager.setProperty(propertyName, value);
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        return entityManager.unwrap(cls);
    }
}
