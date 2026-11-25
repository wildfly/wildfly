/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container.persistence31;

import java.io.Serial;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.jboss.as.jpa.container.ExtendedEntityManager;
import org.jboss.as.jpa.container.QueryNonTxInvocationDetacher;
import org.jboss.as.jpa.container.StoredProcedureQueryNonTxInvocationDetacher;
import org.jboss.as.jpa.container.TransactionScopedEntityManager;
import org.jboss.as.jpa.container.TypedQueryNonTxInvocationDetacher;
import org.jboss.as.jpa.container.UnsynchronizedEntityManagerWrapper;
import org.kohsuke.MetaInfServices;

@MetaInfServices(value = {ExtendedEntityManager.Factory.class, TransactionScopedEntityManager.Factory.class,
        UnsynchronizedEntityManagerWrapper.Factory.class, QueryNonTxInvocationDetacher.Factory.class,
        StoredProcedureQueryNonTxInvocationDetacher.Factory.class, TypedQueryNonTxInvocationDetacher.Factory.class})
public final class WildFlyEntityManagerFactory31
        implements ExtendedEntityManager.Factory, TransactionScopedEntityManager.Factory,
        UnsynchronizedEntityManagerWrapper.Factory, QueryNonTxInvocationDetacher.Factory,
        StoredProcedureQueryNonTxInvocationDetacher.Factory, TypedQueryNonTxInvocationDetacher.Factory {

    @Override
    public ExtendedEntityManager createExtendedEntityManager(String puScopedName, EntityManager underlyingEntityManager, SynchronizationType synchronizationType, TransactionSynchronizationRegistry transactionSynchronizationRegistry, TransactionManager transactionManager) {
        return new ExtendedEntityManager31(puScopedName, underlyingEntityManager, synchronizationType,
                transactionSynchronizationRegistry, transactionManager);
    }

    @Override
    public TransactionScopedEntityManager createTransactionScopedEntityManager(String puScopedName, Map properties, EntityManagerFactory emf, SynchronizationType synchronizationType, TransactionSynchronizationRegistry transactionSynchronizationRegistry, TransactionManager transactionManager) {
        return new TransactionScopedEntityManager31(puScopedName, properties, emf, synchronizationType,
                transactionSynchronizationRegistry, transactionManager);
    }

    @Override
    public UnsynchronizedEntityManagerWrapper createUnsynchronizedEntityManagerWrapper(EntityManager underlyingEntityManager) {
        return new UnsynchronizedEntityManagerWrapper31(underlyingEntityManager);
    }

    @Override
    public QueryNonTxInvocationDetacher createQueryNonTxInvocationDetacher(EntityManager underlyingEntityManager, Query underlyingQuery) {
        return new QueryNonTxInvocationDetacher31(underlyingEntityManager, underlyingQuery);
    }

    @Override
    public StoredProcedureQueryNonTxInvocationDetacher createStoredProcedureQueryNonTxInvocationDetacher(EntityManager underlyingEntityManager, StoredProcedureQuery underlyingQuery) {
        return new StoredProcedureQueryNonTxInvocationDetacher31(underlyingEntityManager, underlyingQuery);
    }

    @Override
    public <X> TypedQueryNonTxInvocationDetacher<X> createTypedQueryNonTxInvocationDetacher(EntityManager underlyingEntityManager, TypedQuery<X> underlyingQuery) {
        return new TypedQueryNonTxInvocationDetacher31<>(underlyingEntityManager, underlyingQuery);
    }

    public static class ExtendedEntityManager31 extends ExtendedEntityManager {

        @Serial
        private static final long serialVersionUID = -4303887788978456675L;

        private ExtendedEntityManager31(String puScopedName, EntityManager underlyingEntityManager, jakarta.persistence.SynchronizationType synchronizationType, jakarta.transaction.TransactionSynchronizationRegistry transactionSynchronizationRegistry, jakarta.transaction.TransactionManager transactionManager) {
            super(puScopedName, underlyingEntityManager, synchronizationType, transactionSynchronizationRegistry, transactionManager);
        }
    }

    public static class TransactionScopedEntityManager31 extends TransactionScopedEntityManager {

        @Serial
        private static final long serialVersionUID = 4060580192670417074L;

        private TransactionScopedEntityManager31(String puScopedName, Map properties, jakarta.persistence.EntityManagerFactory emf, jakarta.persistence.SynchronizationType synchronizationType, jakarta.transaction.TransactionSynchronizationRegistry transactionSynchronizationRegistry, jakarta.transaction.TransactionManager transactionManager) {
            super(puScopedName, properties, emf, synchronizationType, transactionSynchronizationRegistry, transactionManager);
        }
    }

    public static class UnsynchronizedEntityManagerWrapper31 extends UnsynchronizedEntityManagerWrapper {
        private UnsynchronizedEntityManagerWrapper31(EntityManager underlyingEntityManager) {
            super(underlyingEntityManager);
        }
    }

    public static class QueryNonTxInvocationDetacher31 extends QueryNonTxInvocationDetacher {
        private QueryNonTxInvocationDetacher31(EntityManager underlyingEntityManager, Query underlyingQuery) {
            super(underlyingEntityManager, underlyingQuery);
        }
    }

    public static class StoredProcedureQueryNonTxInvocationDetacher31 extends StoredProcedureQueryNonTxInvocationDetacher {
        private StoredProcedureQueryNonTxInvocationDetacher31(EntityManager underlyingEntityManager, StoredProcedureQuery underlyingQuery) {
            super(underlyingEntityManager, underlyingQuery);
        }
    }

    public static class TypedQueryNonTxInvocationDetacher31<X> extends TypedQueryNonTxInvocationDetacher<X> {
        private TypedQueryNonTxInvocationDetacher31(EntityManager underlyingEntityManager, TypedQuery<X> underlyingQuery) {
            super(underlyingEntityManager, underlyingQuery);
        }
    }
}
