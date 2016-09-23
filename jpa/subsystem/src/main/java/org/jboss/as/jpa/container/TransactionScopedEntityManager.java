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

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

import java.io.IOException;
import java.io.Serializable;
import java.security.AccessController;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SynchronizationType;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.messages.JpaLogger;
import org.jboss.as.jpa.service.PersistenceUnitServiceImpl;
import org.jboss.as.jpa.transaction.TransactionUtil;
import org.jboss.as.jpa.util.JPAServiceNames;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.txn.service.TransactionManagerService;
import org.jboss.as.txn.service.TransactionSynchronizationRegistryService;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;

/**
 * Transaction scoped entity manager will be injected into SLSB or SFSB beans.  At bean invocation time, they
 * will join the active transaction if one is present.  Otherwise, they will simply be cleared at the end of
 * the bean invocation.
 * <p/>
 * This is a proxy for the underlying persistent provider EntityManager.
 *
 * @author Scott Marlow
 */
public class TransactionScopedEntityManager extends AbstractEntityManager implements Serializable {

    private static final long serialVersionUID = 455498112L;

    private final String puScopedName;          // Scoped name of the persistent unit
    private final Map properties;
    private transient EntityManagerFactory emf;
    private final SynchronizationType synchronizationType;
    private transient TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private transient TransactionManager transactionManager;
    private transient Boolean deferDetach;

    public TransactionScopedEntityManager(String puScopedName, Map properties, EntityManagerFactory emf, SynchronizationType synchronizationType, TransactionSynchronizationRegistry transactionSynchronizationRegistry, TransactionManager transactionManager) {
        this.puScopedName = puScopedName;
        this.properties = properties;
        this.emf = emf;
        this.synchronizationType = synchronizationType;
        this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
        this.transactionManager = transactionManager;
    }

    @Override
    protected EntityManager getEntityManager() {
        EntityManager entityManager;
        boolean isInTx;

        isInTx = TransactionUtil.isInTx(transactionManager);

        if (isInTx) {
            entityManager = getOrCreateTransactionScopedEntityManager(emf, puScopedName, properties, synchronizationType);
        } else {
            entityManager = NonTxEmCloser.get(puScopedName);
            if (entityManager == null) {
                entityManager = createEntityManager(emf, properties, synchronizationType);
                NonTxEmCloser.add(puScopedName, entityManager);
            }
        }
        return entityManager;
    }

    @Override
    protected boolean isExtendedPersistenceContext() {
        return false;
    }

    @Override
    protected boolean isInTx() {
        return TransactionUtil.isInTx(transactionManager);
    }

    /**
     * Catch the application trying to close the container managed entity manager and throw an IllegalStateException
     */
    @Override
    public void close() {
        // Transaction scoped entity manager will be closed when the (owning) component invocation completes
        throw JpaLogger.ROOT_LOGGER.cannotCloseTransactionContainerEntityManger();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        // read all non-transient fields
        in.defaultReadObject();
        final ServiceController<?> controller = currentServiceContainer().getService(JPAServiceNames.getPUServiceName(puScopedName));
        final PersistenceUnitServiceImpl persistenceUnitService = (PersistenceUnitServiceImpl) controller.getService();
        transactionManager = (TransactionManager) currentServiceContainer().getService(TransactionManagerService.SERVICE_NAME).getValue();
        transactionSynchronizationRegistry = (TransactionSynchronizationRegistry) currentServiceContainer().getService(TransactionSynchronizationRegistryService.SERVICE_NAME).getValue();

        emf = persistenceUnitService.getEntityManagerFactory();
    }

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }

    @Override
    public SynchronizationType getSynchronizationType() {
        return synchronizationType;
    }

    /**
     * get or create a Transactional entity manager.
     * Only call while a transaction is active in the current thread.
     *
     * @param emf
     * @param scopedPuName
     * @param properties
     * @param synchronizationType
     * @return
     */
    private EntityManager getOrCreateTransactionScopedEntityManager(
            final EntityManagerFactory emf,
            final String scopedPuName,
            final Map properties,
            final SynchronizationType synchronizationType) {
        EntityManager entityManager = TransactionUtil.getTransactionScopedEntityManager(puScopedName, transactionSynchronizationRegistry);
        if (entityManager == null) {
            entityManager = createEntityManager(emf, properties, synchronizationType);
            if (ROOT_LOGGER.isDebugEnabled()) {
                ROOT_LOGGER.debugf("%s: created entity manager session %s", TransactionUtil.getEntityManagerDetails(entityManager, scopedPuName),
                        TransactionUtil.getTransaction(transactionManager).toString());
            }
            TransactionUtil.registerSynchronization(entityManager, scopedPuName, transactionSynchronizationRegistry, transactionManager);
            TransactionUtil.putEntityManagerInTransactionRegistry(scopedPuName, entityManager, transactionSynchronizationRegistry);
        }
        else {
            testForMixedSynchronizationTypes(emf, entityManager, puScopedName, synchronizationType, properties);
            if (ROOT_LOGGER.isDebugEnabled()) {
                ROOT_LOGGER.debugf("%s: reuse entity manager session already in tx %s", TransactionUtil.getEntityManagerDetails(entityManager, scopedPuName),
                        TransactionUtil.getTransaction(transactionManager).toString());
            }
        }
        return entityManager;
    }

    private EntityManager createEntityManager(
        EntityManagerFactory emf, Map properties, final SynchronizationType synchronizationType) {
        // only JPA 2.1 applications can specify UNSYNCHRONIZED.
        // Default is SYNCHRONIZED if synchronizationType is not passed to createEntityManager
        if (SynchronizationType.UNSYNCHRONIZED.equals(synchronizationType)) {
            // properties are allowed to be be null in jpa 2.1
            return unsynchronizedEntityManagerWrapper(emf.createEntityManager(synchronizationType, properties));
        }

        if (properties != null && properties.size() > 0) {
            return emf.createEntityManager(properties);
        }
        return emf.createEntityManager();
    }

    private EntityManager unsynchronizedEntityManagerWrapper(EntityManager entityManager) {
        return new UnsynchronizedEntityManagerWrapper(entityManager);
    }

    /**
     * return true if non-tx invocations should defer detaching of entities until entity manager is closed.
     * Note that this is an extension for compatibility with JBoss application server 5.0/6.0 (see AS7-2781)
     */
    @Override
    protected boolean deferEntityDetachUntilClose() {
        if (deferDetach == null)
            deferDetach =
                    (true == Configuration.deferEntityDetachUntilClose(emf.getProperties())? Boolean.TRUE : Boolean.FALSE);
        return deferDetach.booleanValue();
    }

    /**
     * throw error if jta transaction already has an UNSYNCHRONIZED persistence context and a SYNCHRONIZED persistence context
     * is requested.  We are only fussy in this test, if the target component persistence context is SYNCHRONIZED.
     *
     * WFLY-7075 introduces two extensions, allow a (transaction) joined UNSYNCHRONIZED persistence context to be treated as SYNCHRONIZED,
     * allow the checking for mixed SynchronizationType to be skipped.
     */
    private static void testForMixedSynchronizationTypes(EntityManagerFactory emf, EntityManager entityManagerFromJTA, String scopedPuName, final SynchronizationType targetSynchronizationType, Map targetProperties) {

        boolean skipMixedSyncTypeChecking = Configuration.skipMixedSynchronizationTypeCheck(emf, targetProperties);  // extension to allow skipping of check based on properties of target entity manager
        boolean allowJoinedUnsyncPersistenceContext = Configuration.allowJoinedUnsyncPersistenceContext(emf, targetProperties); // extension to allow joined unsync persistence context to be treated as sync persistence context

        if (!skipMixedSyncTypeChecking &&
                SynchronizationType.SYNCHRONIZED.equals(targetSynchronizationType) &&
                entityManagerFromJTA instanceof SynchronizationTypeAccess &&
                SynchronizationType.UNSYNCHRONIZED.equals(((SynchronizationTypeAccess) entityManagerFromJTA).getSynchronizationType())
                && (!allowJoinedUnsyncPersistenceContext || !entityManagerFromJTA.isJoinedToTransaction())) {
            throw JpaLogger.ROOT_LOGGER.badSynchronizationTypeCombination(scopedPuName);
        }
    }
}
