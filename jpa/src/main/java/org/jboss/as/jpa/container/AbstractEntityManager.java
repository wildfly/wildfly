/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract entity manager used by all container managed entity managers.
 *
 * @author Scott Marlow (forked from jboss-jpa)
 */
public abstract class AbstractEntityManager implements EntityManager {

    private static final long serialVersionUID = 3L;

    private final Map<Class, Object> extensions = new HashMap<Class, Object>();

    protected abstract EntityManager getEntityManager();

    /**
     * save metadata if not already set.
     * @param puScopedName
     * @param isExtendedPersistenceContext
     */
    protected void setMetadata(
        String puScopedName,
        boolean isExtendedPersistenceContext) {
        if (extensions.get(EntityManagerMetadata.class) == null) {
            EntityManagerMetadata metadata = new EntityManagerMetadata();
            metadata.setScopedPuName(puScopedName);
            metadata.setTransactionScopedEntityManager(!isExtendedPersistenceContext);
            addExtension(EntityManagerMetadata.class, metadata);
        }
    }

    /**
     * Add an extension for unwrap
     *
     * @param cls       is the Class that extension will be retrieved with on future calls to unwrap.
     * @param extension is the extension object to be returned from unwrap.
     */
    protected void addExtension(Class cls, Object extension) {
        extensions.put(cls, extension);
    }

    public <T> T unwrap(Class<T> cls) {
        Object x = extensions.get(cls);
        if (x != null)
            return (T) x;
        return getEntityManager().unwrap(cls);
    }

    public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        return getEntityManager().createNamedQuery(name, resultClass);
    }

    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        return getEntityManager().createQuery(criteriaQuery);
    }

    public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        return getEntityManager().createQuery(qlString, resultClass);
    }

    public void detach(Object entity) {
        getEntityManager().detach(entity);
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
        return getEntityManager().find(entityClass, primaryKey, properties);
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
        return getEntityManager().find(entityClass, primaryKey, lockMode);
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
        return getEntityManager().find(entityClass, primaryKey, lockMode, properties);
    }

    public CriteriaBuilder getCriteriaBuilder() {
        return getEntityManager().getCriteriaBuilder();
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return getEntityManager().getEntityManagerFactory();
    }

    public LockModeType getLockMode(Object entity) {
        return getEntityManager().getLockMode(entity);
    }

    public Metamodel getMetamodel() {
        return getEntityManager().getMetamodel();
    }

    public Map<String, Object> getProperties() {
        return getEntityManager().getProperties();
    }

    public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        getEntityManager().lock(entity, lockMode, properties);
    }

    public void refresh(Object entity, Map<String, Object> properties) {
        getEntityManager().refresh(entity, properties);
    }

    public void refresh(Object entity, LockModeType lockMode) {
        getEntityManager().refresh(entity, lockMode);
    }

    public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        getEntityManager().refresh(entity, lockMode, properties);
    }

    public void setProperty(String propertyName, Object value) {
        getEntityManager().setProperty(propertyName, value);
    }

    public void clear() {
        getEntityManager().clear();
    }

    public void close() {
        getEntityManager().close();
    }

    public boolean contains(Object entity) {
        return getEntityManager().contains(entity);
    }

    public Query createNamedQuery(String name) {
        return getEntityManager().createNamedQuery(name);
    }

    @SuppressWarnings("unchecked")
    public Query createNativeQuery(String sqlString, Class resultClass) {
        return getEntityManager().createNativeQuery(sqlString, resultClass);
    }

    public Query createNativeQuery(String sqlString, String resultSetMapping) {
        return getEntityManager().createNativeQuery(sqlString, resultSetMapping);
    }

    public Query createNativeQuery(String sqlString) {
        return getEntityManager().createNativeQuery(sqlString);
    }

    public Query createQuery(String ejbqlString) {
        return getEntityManager().createQuery(ejbqlString);
    }

    public <T> T find(Class<T> entityClass, Object primaryKey) {
        return getEntityManager().find(entityClass, primaryKey);
    }

    public void flush() {
        getEntityManager().flush();
    }

    public Object getDelegate() {
        return getEntityManager().getDelegate();
    }

    public FlushModeType getFlushMode() {
        return getEntityManager().getFlushMode();
    }

    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        return getEntityManager().getReference(entityClass, primaryKey);
    }

    public EntityTransaction getTransaction() {
        return getEntityManager().getTransaction();
    }

    public boolean isOpen() {
        return getEntityManager().isOpen();
    }

    public void joinTransaction() {
        getEntityManager().joinTransaction();
    }

    public void lock(Object entity, LockModeType lockMode) {
        getEntityManager().lock(entity, lockMode);
    }

    public <T> T merge(T entity) {
        return getEntityManager().merge(entity);
    }

    public void persist(Object entity) {
        getEntityManager().persist(entity);
    }

    public void refresh(Object entity) {
        getEntityManager().refresh(entity);
    }

    public void remove(Object entity) {
        getEntityManager().remove(entity);
    }

    public void setFlushMode(FlushModeType flushMode) {
        getEntityManager().setFlushMode(flushMode);
    }
}
