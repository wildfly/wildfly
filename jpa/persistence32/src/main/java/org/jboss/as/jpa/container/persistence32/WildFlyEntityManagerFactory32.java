/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container.persistence32;

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
public final class WildFlyEntityManagerFactory32
        implements ExtendedEntityManager.Factory, TransactionScopedEntityManager.Factory,
        UnsynchronizedEntityManagerWrapper.Factory, QueryNonTxInvocationDetacher.Factory,
        StoredProcedureQueryNonTxInvocationDetacher.Factory, TypedQueryNonTxInvocationDetacher.Factory {

    @Override
    public ExtendedEntityManager createExtendedEntityManager(String puScopedName, EntityManager underlyingEntityManager, SynchronizationType synchronizationType, TransactionSynchronizationRegistry transactionSynchronizationRegistry, TransactionManager transactionManager) {
        return new ExtendedEntityManager32(puScopedName, underlyingEntityManager, synchronizationType,
                transactionSynchronizationRegistry, transactionManager);
    }

    @Override
    public TransactionScopedEntityManager createTransactionScopedEntityManager(String puScopedName, Map properties, EntityManagerFactory emf, SynchronizationType synchronizationType, TransactionSynchronizationRegistry transactionSynchronizationRegistry, TransactionManager transactionManager) {
        return new TransactionScopedEntityManager32(puScopedName, properties, emf, synchronizationType,
                transactionSynchronizationRegistry, transactionManager);
    }

    @Override
    public UnsynchronizedEntityManagerWrapper createUnsynchronizedEntityManagerWrapper(EntityManager underlyingEntityManager) {
        return new UnsynchronizedEntityManagerWrapper32(underlyingEntityManager);
    }

    @Override
    public QueryNonTxInvocationDetacher createQueryNonTxInvocationDetacher(EntityManager underlyingEntityManager, Query underlyingQuery) {
        return new QueryNonTxInvocationDetacher32(underlyingEntityManager, underlyingQuery);
    }

    @Override
    public StoredProcedureQueryNonTxInvocationDetacher createStoredProcedureQueryNonTxInvocationDetacher(EntityManager underlyingEntityManager, StoredProcedureQuery underlyingQuery) {
        return new StoredProcedureQueryNonTxInvocationDetacher32(underlyingEntityManager, underlyingQuery);
    }

    @Override
    public <X> TypedQueryNonTxInvocationDetacher<X> createTypedQueryNonTxInvocationDetacher(EntityManager underlyingEntityManager, TypedQuery<X> underlyingQuery) {
        return new TypedQueryNonTxInvocationDetacher32<>(underlyingEntityManager, underlyingQuery);
    }
}
