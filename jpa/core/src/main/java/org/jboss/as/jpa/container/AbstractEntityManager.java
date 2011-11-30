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

import static org.jboss.as.jpa.JpaLogger.ROOT_LOGGER;
import static org.jboss.as.jpa.JpaMessages.MESSAGES;

import java.util.HashMap;
import java.util.Map;

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

/**
 * Abstract entity manager used by all container managed entity managers.
 *
 * @author Scott Marlow (forked from jboss-jpa)
 */
public abstract class AbstractEntityManager implements EntityManager {
    private final boolean isTraceEnabled = ROOT_LOGGER.isTraceEnabled();
    private final Map<Class, Object> extensions = new HashMap<Class, Object>();

    protected AbstractEntityManager(final String puScopedName, final boolean isExtendedPersistenceContext) {
        setMetadata(puScopedName, isExtendedPersistenceContext);
    }

    protected abstract EntityManager getEntityManager();

    /**
     * @return true if an extended persistence context is in use
     *         <p/>
     *         Precondition: getEntityManager() must be called previous to calling isExtendedPersistenceContext
     */
    protected abstract boolean isExtendedPersistenceContext();

    /**
     * @return true if a JTA transaction is active
     *         <p/>
     *         Precondition: getEntityManager() must be called previous to calling isInTx
     */
    protected abstract boolean isInTx();

    /**
     * save metadata if not already set.
     *
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
        return getEntityManager().unwrap(cls);
    }

    public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            return getEntityManager().createNamedQuery(name, resultClass);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("createNamedQuery name '%s', resultClass '%s' took %dms", name, resultClass.getName(), elapsed);
            }
        }
    }

    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            return getEntityManager().createQuery(criteriaQuery);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("createQuery took %dms", elapsed);
            }
        }
    }

    public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            return getEntityManager().createQuery(qlString, resultClass);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("createQuery resultClass '%s' took %dms", resultClass.getName(), elapsed);
            }
        }
    }

    public void detach(Object entity) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            getEntityManager().detach(entity);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("detach entityClass '%s' took %dms", entity.getClass().getName(), elapsed);
            }
        }
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            final EntityManager underlyingEntityManager = getEntityManager();
            T result = underlyingEntityManager.find(entityClass, primaryKey, properties);
            detachNonTxInvocation(underlyingEntityManager);
            return result;
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("find entityClass '%s' took %dms", entityClass.getName(), elapsed);
            }
        }
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            final EntityManager underlyingEntityManager = getEntityManager();
            T result = underlyingEntityManager.find(entityClass, primaryKey, lockMode);
            detachNonTxInvocation(underlyingEntityManager);
            return result;
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("find entityClass '%s', lockMode '%s' took %dms", entityClass.getName(), getLockModeAsString(lockMode), elapsed);
            }
        }
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            final EntityManager underlyingEntityManager = getEntityManager();
            T result = underlyingEntityManager.find(entityClass, primaryKey, lockMode, properties);
            detachNonTxInvocation(underlyingEntityManager);
            return result;
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("find entityClass '%s', lockMode '%s' took %dms", entityClass.getName(), getLockModeAsString(lockMode), elapsed);
            }
        }
    }

    public <T> T find(Class<T> entityClass, Object primaryKey) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            final EntityManager underlyingEntityManager = getEntityManager();
            T result = getEntityManager().find(entityClass, primaryKey);
            detachNonTxInvocation(underlyingEntityManager);
            return result;
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("find entityClass '%s' took %dms", entityClass.getName(), elapsed);
            }
        }
    }


    public CriteriaBuilder getCriteriaBuilder() {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            return getEntityManager().getCriteriaBuilder();
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("getCriteriaBuilder took %dms", elapsed);
            }
        }
    }

    public EntityManagerFactory getEntityManagerFactory() {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            return getEntityManager().getEntityManagerFactory();
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("getEntityManagerFactory took %dms", elapsed);
            }
        }
    }

    public LockModeType getLockMode(Object entity) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        LockModeType result = null;
        try {
            result = getEntityManager().getLockMode(entity);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("getLockMode entityClass '%s', lockMode '%s'  took %dms", entity.getClass().getName(), getLockModeAsString(result), elapsed);
            }
        }
        return result;
    }

    public Metamodel getMetamodel() {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            return getEntityManager().getMetamodel();
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("getMetamodel took %dms", elapsed);
            }
        }
    }

    public Map<String, Object> getProperties() {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            return getEntityManager().getProperties();
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("getProperties took %dms", elapsed);
            }
        }
    }

    public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            getEntityManager().lock(entity, lockMode, properties);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("lock entityClass '%s', lockMode '%s'  took %dms", entity.getClass().getName(), getLockModeAsString(lockMode), elapsed);
            }
        }
    }


    public void setProperty(String propertyName, Object value) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            getEntityManager().setProperty(propertyName, value);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("setProperty took %dms", elapsed);
            }
        }
    }

    public void clear() {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            getEntityManager().clear();
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("clear took %dms", elapsed);
            }
        }
    }

    public void close() {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            getEntityManager().close();
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("close took %dms", elapsed);
            }
        }
    }

    public boolean contains(Object entity) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            return getEntityManager().contains(entity);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("contains '%s' took %dms", entity.getClass().getName(), elapsed);
            }
        }
    }

    public Query createNamedQuery(String name) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            return getEntityManager().createNamedQuery(name);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("createNamedQuery name '%s' took %dms", name, elapsed);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Query createNativeQuery(String sqlString, Class resultClass) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            return getEntityManager().createNativeQuery(sqlString, resultClass);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("createNativeQuery resultClass '%s' took %dms", resultClass.getName(), elapsed);
            }
        }
    }

    public Query createNativeQuery(String sqlString, String resultSetMapping) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            return getEntityManager().createNativeQuery(sqlString, resultSetMapping);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("createNativeQuery took %dms", elapsed);
            }
        }
    }

    public Query createNativeQuery(String sqlString) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            return getEntityManager().createNativeQuery(sqlString);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("createNativeQuery took %dms", elapsed);
            }
        }
    }

    public Query createQuery(String ejbqlString) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            return getEntityManager().createQuery(ejbqlString);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("createQuery took %dms", elapsed);
            }
        }
    }

    public void flush() {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            getEntityManager().flush();
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("flush took %dms", elapsed);
            }
        }
    }

    public Object getDelegate() {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            return getEntityManager().getDelegate();
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("getDelegate took %dms", elapsed);
            }
        }
    }

    public FlushModeType getFlushMode() {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            return getEntityManager().getFlushMode();
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("getFlushMode took %dms", elapsed);
            }
        }
    }

    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            final EntityManager underlyingEntityManager = getEntityManager();
            T result = getEntityManager().getReference(entityClass, primaryKey);
            detachNonTxInvocation(underlyingEntityManager);
            return result;
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("getReference entityClass '%s' took %dms", entityClass.getName(), elapsed);
            }
        }
    }

    public EntityTransaction getTransaction() {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            return getEntityManager().getTransaction();
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("getTransaction took %dms", elapsed);
            }
        }
    }

    public boolean isOpen() {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        return getEntityManager().isOpen();
    }

    public void joinTransaction() {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            getEntityManager().joinTransaction();
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("joinTransaction took %dms", elapsed);
            }
        }
    }

    public void lock(Object entity, LockModeType lockMode) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            getEntityManager().lock(entity, lockMode);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("lock entityClass '%s', lockMode '%s' took %dms", entity.getClass().getName(), getLockModeAsString(lockMode), elapsed);
            }
        }
    }

    public <T> T merge(T entity) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            transactionIsRequired();
            return getEntityManager().merge(entity);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("merge entityClass '%s' took %dms", entity.getClass().getName(), elapsed);
            }
        }
    }

    public void persist(Object entity) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            transactionIsRequired();
            getEntityManager().persist(entity);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("persist entityClass '%s' took %dms", entity.getClass().getName(), elapsed);
            }
        }
    }

    public void refresh(Object entity) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            transactionIsRequired();
            getEntityManager().refresh(entity);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("refresh entityClass '%s' took %dms", entity.getClass().getName(), elapsed);
            }
        }
    }

    public void refresh(Object entity, Map<String, Object> properties) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            transactionIsRequired();
            getEntityManager().refresh(entity, properties);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("refresh entityClass '%s' took %dms", entity.getClass().getName(), elapsed);
            }
        }
    }

    public void refresh(Object entity, LockModeType lockMode) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            transactionIsRequired();
            getEntityManager().refresh(entity, lockMode);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("refresh entityClass '%s', lockMode '%s' took %dms", entity.getClass().getName(), getLockModeAsString(lockMode), elapsed);
            }
        }
    }

    public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            transactionIsRequired();
            getEntityManager().refresh(entity, lockMode, properties);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("refresh entityClass '%s', lockMode '%s' took %dms", entity.getClass().getName(), getLockModeAsString(lockMode), elapsed);
            }
        }
    }

    public void remove(Object entity) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            transactionIsRequired();
            getEntityManager().remove(entity);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("remove entityClass '%s' took %dms", entity.getClass().getName(), elapsed);
            }
        }
    }

    public void setFlushMode(FlushModeType flushMode) {
        long start = 0;
        if (isTraceEnabled)
            start = System.currentTimeMillis();
        try {
            getEntityManager().setFlushMode(flushMode);
        } finally {
            if (isTraceEnabled) {
                long elapsed = System.currentTimeMillis() - start;
                ROOT_LOGGER.tracef("setFlushMode took %dms", elapsed);
            }
        }
    }

    // perform any cleanup needed after an invocation.
    // currently used by TransactionScopedEntityManager to autoclose the
    // underlying entitymanager after each invocation.
    protected void detachNonTxInvocation(EntityManager underlyingEntityManager) {
        if (!this.isExtendedPersistenceContext() && !this.isInTx()) {
            underlyingEntityManager.clear();
        }
    }

    // JPA 7.9.1 if invoked without a JTA transaction and a transaction scoped persistence context is used,
    // will throw TransactionRequiredException for any calls to entity manager remove/merge/persist/refresh.
    private void transactionIsRequired() {
        if (!this.isExtendedPersistenceContext() && !this.isInTx()) {
            throw MESSAGES.transactionRequired();
        }
    }

    private static String getLockModeAsString(LockModeType lockMode) {
        if (lockMode == null)
            return "(null)";
        switch (lockMode) {
            case OPTIMISTIC:
                return "optimistic";
            case OPTIMISTIC_FORCE_INCREMENT:
                return "optimistic_force_increment";
            case READ:
                return "read";
            case WRITE:
                return "write";
            case PESSIMISTIC_READ:
                return "pessimistic_read";
            case PESSIMISTIC_FORCE_INCREMENT:
                return "pessimistic_force_increment";
            default:
            case NONE:
                return "none";
        }
    }


}
