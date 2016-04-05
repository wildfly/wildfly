/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.jpa.container;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.SynchronizationType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;

/**
 * UnsynchronizedEntityManagerWrapper helps track transaction scoped persistence contexts that are SynchronizationType.UNSYNCHRONIZED,
 * for error checking.
 *
 * If an SynchronizationType.SYNCHRONIZED transaction scoped persistence context is accessed
 * while there is already an SynchronizationType.UNSYNCHRONIZED (with the same pu name + in active JTA TX),
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
