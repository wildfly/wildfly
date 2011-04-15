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
import javax.persistence.TransactionRequiredException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Abstract entity manager used by all container managed entity managers.
 *
 * @author Scott Marlow (forked from jboss-jpa)
 */
public abstract class AbstractEntityManager implements EntityManager {

    // the following list of classes determines which unwrap classes are special, in that the underlying entity
    // manager won't be closed, even if no transaction is active on the calling thread.
    // TODO:  move this list to PersistenceProviderAdaptor
    private static final HashSet<String> unwrapClassNamesThatShouldSkipPostInvocationStep = new HashSet<String>();
    static {
        unwrapClassNamesThatShouldSkipPostInvocationStep.add("org.hibernate.Session");
    }

    private final Map<Class, Object> extensions = new HashMap<Class, Object>();

    protected AbstractEntityManager(final String puScopedName, final boolean isExtendedPersistenceContext) {
        setMetadata(puScopedName, isExtendedPersistenceContext);
    }

    protected abstract EntityManager getEntityManager();

    /**
     * @return true if an extended persistence context is in use
     *
     * Precondition: getEntityManager() must be called previous to calling isExtendedPersistenceContext
     */
    protected abstract boolean isExtendedPersistenceContext();

    /**
     * @return true if a JTA transaction is active
     *
     * Precondition: getEntityManager() must be called previous to calling isInTx
     */
    protected abstract boolean isInTx();
    /**
     * save metadata if not already set.
     * @param puScopedName
     * @param isExtendedPersistenceContext
     */
    private void setMetadata(
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

        final EntityManager underlyingEntityManager = getEntityManager();

        // postinvocation is currently used specifically for closing transactional entity manager not running in tx
        // check if we should skip the post invocation notification.
        if (unwrapClassNamesThatShouldSkipPostInvocationStep.contains(cls.getName())) {
            return underlyingEntityManager.unwrap(cls);
        }

        RuntimeException exceptionWasAlreadyThrown=null;
        T result = null;
        try {
            result = underlyingEntityManager.unwrap(cls);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        TypedQuery<T> result = null;
        try {
            result = underlyingEntityManager.createNamedQuery(name, resultClass);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        TypedQuery<T> result = null;
        try {
            result = underlyingEntityManager.createQuery(criteriaQuery);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        TypedQuery<T> result = null;
        try {
            result = underlyingEntityManager.createQuery(qlString, resultClass);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public void detach(Object entity) {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        try {
            underlyingEntityManager.detach(entity);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        T result = null;
        try {
            result = underlyingEntityManager.find(entityClass, primaryKey, properties);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        T result = null;
        try {
            result = underlyingEntityManager.find(entityClass, primaryKey, lockMode);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        T result = null;
        try {
            result = underlyingEntityManager.find(entityClass, primaryKey, lockMode, properties);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public CriteriaBuilder getCriteriaBuilder() {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        CriteriaBuilder result = null;
        try {
            result = underlyingEntityManager.getCriteriaBuilder();
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public EntityManagerFactory getEntityManagerFactory() {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        EntityManagerFactory result = null;
        try {
            result = underlyingEntityManager.getEntityManagerFactory();
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public LockModeType getLockMode(Object entity) {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        LockModeType result = null;
        try {
            result = underlyingEntityManager.getLockMode(entity);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public Metamodel getMetamodel() {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        Metamodel result = null;
        try {
            result = underlyingEntityManager.getMetamodel();
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public Map<String, Object> getProperties() {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        Map<String, Object> result = null;
        try {
            result = underlyingEntityManager.getProperties();
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        try {
            underlyingEntityManager.lock(entity, lockMode, properties);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
    }

    public void setProperty(String propertyName, Object value) {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        try {
            underlyingEntityManager.setProperty(propertyName, value);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
    }

    public void clear() {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        try {
            underlyingEntityManager.clear();
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
    }

    public void close() {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        try {
            underlyingEntityManager.close();
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
    }

    public boolean contains(Object entity) {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        boolean result = false;
        try {
            result = underlyingEntityManager.contains(entity);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public Query createNamedQuery(String name) {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        Query result = null;
        try {
            result = underlyingEntityManager.createNamedQuery(name);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public Query createNativeQuery(String sqlString, Class resultClass) {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        Query result = null;
        try {
            result = underlyingEntityManager.createNativeQuery(sqlString, resultClass);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public Query createNativeQuery(String sqlString, String resultSetMapping) {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        Query result = null;
        try {
            result = underlyingEntityManager.createNativeQuery(sqlString, resultSetMapping);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public Query createNativeQuery(String sqlString) {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        Query result = null;
        try {
            result = underlyingEntityManager.createNativeQuery(sqlString);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public Query createQuery(String ejbqlString) {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        Query result = null;
        try {
            result = underlyingEntityManager.createQuery(ejbqlString);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public <T> T find(Class<T> entityClass, Object primaryKey) {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        T result = null;
        try {
            result = underlyingEntityManager.find(entityClass, primaryKey);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public void flush() {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        try {
            underlyingEntityManager.flush();
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
    }

    public Object getDelegate() {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        Object result = null;
        try {
            result = underlyingEntityManager.getDelegate();
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public FlushModeType getFlushMode() {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        FlushModeType result = null;
        try {
            result = underlyingEntityManager.getFlushMode();
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        T result = null;
        try {
            result = underlyingEntityManager.getReference(entityClass, primaryKey);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public EntityTransaction getTransaction() {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        EntityTransaction result = null;
        try {
            result = underlyingEntityManager.getTransaction();
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public boolean isOpen() {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        boolean result = false;
        try {
            result = underlyingEntityManager.isOpen();
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public void joinTransaction() {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        try {
            underlyingEntityManager.joinTransaction();
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
    }

    public void lock(Object entity, LockModeType lockMode) {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        try {
            underlyingEntityManager.lock(entity, lockMode);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
    }

    public <T> T merge(T entity) {
        EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        T result = null;
        try {
            transactionIsRequired();
            result = underlyingEntityManager.merge(entity);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
        return result;
    }

    public void persist(Object entity) {
        EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        try {
            transactionIsRequired();
            underlyingEntityManager.persist(entity);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
    }

    public void refresh(Object entity) {
        EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        try {
            transactionIsRequired();
            underlyingEntityManager.refresh(entity);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
    }

    public void refresh(Object entity, Map<String, Object> properties) {
        EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        try {
            transactionIsRequired();
            underlyingEntityManager.refresh(entity, properties);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
    }

    public void refresh(Object entity, LockModeType lockMode) {
        EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        try {
            transactionIsRequired();
            underlyingEntityManager.refresh(entity, lockMode);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
    }

    public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        try {
            transactionIsRequired();
            underlyingEntityManager.refresh(entity, lockMode, properties);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
    }

    public void remove(Object entity) {
        EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        try {
            transactionIsRequired();
            underlyingEntityManager.remove(entity);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
    }

    public void setFlushMode(FlushModeType flushMode) {
        final EntityManager underlyingEntityManager = getEntityManager();
        RuntimeException exceptionWasAlreadyThrown=null;
        try {
            underlyingEntityManager.setFlushMode(flushMode);
        } catch(RuntimeException re) {
            exceptionWasAlreadyThrown = re;
        } finally {
            postInvocation(underlyingEntityManager, exceptionWasAlreadyThrown);
        }
    }

    // perform any cleanup needed after an invocation.
    // currently used by TransactionScopedEntityManager to autoclose the
    // underlying entitymanager after each invocation.
    protected void postInvocation(
        EntityManager underlyingEntityManager, RuntimeException exceptionWasAlreadyThrown) {
        if (exceptionWasAlreadyThrown != null) {
            throw exceptionWasAlreadyThrown;
        }
    }

    // JPA 7.9.1 if invoked without a JTA transaction and a transaction scoped persistence context is used,
    // will throw TransactionRequiredException for any calls to entity manager remove/merge/persist/refresh.
    private void transactionIsRequired() {
        if ( ! this.isExtendedPersistenceContext() && ! this.isInTx()) {
            throw new TransactionRequiredException(
                "Transaction is required to perform this operation (either use a transaction or extended persistence context)");
        }
    }

}
