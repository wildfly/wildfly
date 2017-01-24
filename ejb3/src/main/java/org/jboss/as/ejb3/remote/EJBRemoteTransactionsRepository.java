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

package org.jboss.as.ejb3.remote;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinateTransaction;
import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinationManager;
import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.TransactionImporter;
import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.TransactionImporterImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.XATerminatorImple;
import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.suspend.EJBSuspendHandlerService;
import org.jboss.ejb.client.TransactionID;
import org.jboss.ejb.client.UserTransactionID;
import org.jboss.ejb.client.XidTransactionID;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.tm.ExtendedJBossXATerminator;
import org.wildfly.security.manager.WildFlySecurityManager;

import javax.resource.spi.XATerminator;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * @author Jaikiran Pai
 * @author Flavia Rainone
 */
public class EJBRemoteTransactionsRepository implements Service<EJBRemoteTransactionsRepository> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb").append("remote-transactions-repository");

    private final InjectedValue<TransactionManager> transactionManagerInjectedValue = new InjectedValue<TransactionManager>();

    private final InjectedValue<UserTransaction> userTransactionInjectedValue = new InjectedValue<UserTransaction>();

    private final InjectedValue<RecoveryManagerService> recoveryManagerService = new InjectedValue<>();

    private final InjectedValue<ExtendedJBossXATerminator> xaTerminatorInjectedValue = new InjectedValue<>();

    private final InjectedValue<EJBSuspendHandlerService> ejbSuspendHandlerServiceInjectedValue = new InjectedValue<>();

    private final Map<UserTransactionID, Uid> userTransactions = Collections.synchronizedMap(new HashMap<UserTransactionID, Uid>());

    private static final Xid[] NO_XIDS = new Xid[0];
    private static final boolean RECOVER_IN_FLIGHT;

    // count keeping track of active transactions
    @SuppressWarnings("unused")
    private volatile int activeTransactionCount = 0;


    private static final AtomicIntegerFieldUpdater<EJBRemoteTransactionsRepository> activeTransactionCountUpdater = AtomicIntegerFieldUpdater
            .newUpdater(EJBRemoteTransactionsRepository.class, "activeTransactionCount");


    static {
        RECOVER_IN_FLIGHT = Boolean.parseBoolean(WildFlySecurityManager.getPropertyPrivileged("org.wildfly.ejb.txn.recovery.in-flight", "false"));
    }

    @Override
    public void start(StartContext context) throws StartException {
        recoveryManagerService.getValue().addSerializableXAResourceDeserializer(EJBXAResourceDeserializer.INSTANCE);
        EjbLogger.REMOTE_LOGGER.debugf("Registered EJB XA resource deserializer %s", EJBXAResourceDeserializer.INSTANCE);
        getEJBSuspendHandlerService().setEJBRemoteTransactionsRepository(this);
    }

    @Override
    public void stop(StopContext context) {
        getEJBSuspendHandlerService().setEJBRemoteTransactionsRepository(null);
    }

    @Override
    public EJBRemoteTransactionsRepository getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public TransactionManager getTransactionManager() {
        return this.transactionManagerInjectedValue.getValue();
    }

    public ExtendedJBossXATerminator getXATerminator() {
        return this.xaTerminatorInjectedValue.getValue();
    }

    /**
     * Removes any references maintained for the passed <code>{@link UserTransactionID}</code>
     * @param userTransactionID User transaction id
     * @return Returns the {@link Transaction} corresponding to the passed <code>userTransactionID</code>. If there
     *          is no such transaction, then this method returns null
     */
    public Transaction removeUserTransaction(final UserTransactionID userTransactionID) {
        final Uid uid = this.userTransactions.remove(userTransactionID);
        if (uid == null) {
            return null;
        }
        decrementActiveTransactionCount();
        return TransactionImple.getTransaction(uid);
    }

    /**
     * @param userTransactionID User transaction id
     * @return Returns the {@link Transaction} corresponding to the passed <code>userTransactionID</code>. If there
     *          is no such transaction, then this method returns null
     */
    public Transaction getUserTransaction(final UserTransactionID userTransactionID) {
        final Uid uid = this.userTransactions.get(userTransactionID);
        if (uid == null) {
            return null;
        }
        return TransactionImple.getTransaction(uid);
    }

    private EJBSuspendHandlerService getEJBSuspendHandlerService() {
        return ejbSuspendHandlerServiceInjectedValue.getValue();
    }

    /**
     * {@link javax.transaction.UserTransaction#begin() Begins} a new {@link UserTransaction} and
     * associates it with the passed {@link UserTransactionID}.
     * @param userTransactionID
     * @return Returns the transaction that has begun
     * @throws SystemException
     * @throws NotSupportedException
     */
    Transaction beginUserTransaction(final UserTransactionID userTransactionID) throws SystemException, NotSupportedException {
        if (getEJBSuspendHandlerService().isSuspended()) {
            throw EjbLogger.ROOT_LOGGER.cannotBeginUserTransaction();
        }
        this.getUserTransaction().begin();
        activeTransactionCountUpdater.incrementAndGet(this);
        // get the tx that just got created and associated with the transaction manager
        final TransactionImple newlyAssociatedTx = TransactionImple.getTransaction();
        final Uid uid = newlyAssociatedTx.get_uid();
        this.userTransactions.put(userTransactionID, uid);
        return newlyAssociatedTx;
    }

    /**
     * Returns a {@link SubordinateTransaction} associated with the passed {@link XidTransactionID}.
     * If there's no such transaction, then this method returns null.
     *
     * @param xidTransactionID The {@link XidTransactionID}
     * @return
     * @throws XAException
     */
    public SubordinateTransaction getImportedTransaction(final XidTransactionID xidTransactionID) throws XAException {
        final Xid xid = xidTransactionID.getXid();
        final TransactionImporter transactionImporter = SubordinationManager.getTransactionImporter();
        return transactionImporter.getImportedTransaction(xid);
    }

    /**
     * Removes a {@link SubordinateTransaction} associated with the passed {@link XidTransactionID}.
     * If there's no such transaction, then this method is ignored.
     *
     * @param xidTransactionID The {@link XidTransactionID}
     * @return
     * @throws XAException
     */
    public void removeImportedTransaction(final XidTransactionID xidTransactionID) throws XAException {
        final Xid xid = xidTransactionID.getXid();
        final TransactionImporter transactionImporter = SubordinationManager.getTransactionImporter();
        // can we have a boolean indicating if the transaction was removed
        transactionImporter.removeImportedTransaction(xid);
        decrementActiveTransactionCount();

    }

    private void decrementActiveTransactionCount() {
        if (activeTransactionCountUpdater.decrementAndGet(this) == 0) {
            getEJBSuspendHandlerService().noActiveTransactions();
        }
    }

    /**
     * Imports a {@link Transaction} into the {@link SubordinationManager} and associates it with the
     * passed {@link org.jboss.ejb.client.XidTransactionID#getXid()}  Xid}. Returns the imported transaction
     *
     * @param xidTransactionID The {@link XidTransactionID}
     * @param txTimeout The transaction timeout
     * @return
     * @throws XAException
     */
    Transaction importTransaction(final XidTransactionID xidTransactionID, final int txTimeout) throws XAException {
        final TransactionImporter transactionImporter = SubordinationManager.getTransactionImporter();
        activeTransactionCountUpdater.incrementAndGet(this);
        return transactionImporter.importTransaction(xidTransactionID.getXid(), txTimeout);
    }

    public Xid[] getXidsToRecoverForParentNode(final String parentNodeName, int recoveryFlags) throws XAException {
        final Set<Xid> xidsToRecover = new HashSet<Xid>();
        if (RECOVER_IN_FLIGHT) {
            final TransactionImporter transactionImporter = SubordinationManager.getTransactionImporter();
            if (transactionImporter instanceof TransactionImporterImple) {
                final Set<Xid> inFlightXids = ((TransactionImporterImple) transactionImporter).getInflightXids(parentNodeName);
                if (inFlightXids != null) {
                    xidsToRecover.addAll(inFlightXids);
                }
            }
        }
        final XATerminator xaTerminator = SubordinationManager.getXATerminator();
        if (xaTerminator instanceof XATerminatorImple) {
            final Xid[] inDoubtTransactions = ((XATerminatorImple) xaTerminator).doRecover(null, parentNodeName);
            if (inDoubtTransactions != null) {
                xidsToRecover.addAll(Arrays.asList(inDoubtTransactions));
            }
        } else {
            final Xid[] inDoubtTransactions = xaTerminator.recover(recoveryFlags);
            if (inDoubtTransactions != null) {
                xidsToRecover.addAll(Arrays.asList(inDoubtTransactions));
            }
        }
        return xidsToRecover.toArray(NO_XIDS);
    }

    public UserTransaction getUserTransaction() {
        return this.userTransactionInjectedValue.getValue();
    }

    public Injector<TransactionManager> getTransactionManagerInjector() {
        return this.transactionManagerInjectedValue;
    }

    public Injector<UserTransaction> getUserTransactionInjector() {
        return this.userTransactionInjectedValue;
    }

    public Injector<RecoveryManagerService> getRecoveryManagerInjector() {
        return this.recoveryManagerService;
    }

    public Injector<ExtendedJBossXATerminator> getXATerminatorInjector() {
        return this.xaTerminatorInjectedValue;
    }

    public Injector<EJBSuspendHandlerService> getEJBSuspendHandlerServiceInjector() {
        return this.ejbSuspendHandlerServiceInjectedValue;
    }


    public int getActiveTransactionCount() {
        return activeTransactionCountUpdater.get(this);
    }

    /**
     * Check if this remote transaction is active in this server.
     *
     * @param transactionID the transaction id
     * @return {@code true} if the transaction is active in this server
     */
    public boolean isRemoteTransactionActive(TransactionID transactionID) {
        if (transactionID != null) {
            // if it's UserTransaction then create or resume the UserTransaction corresponding to the ID
            if (transactionID instanceof UserTransactionID) {
                return getUserTransaction((UserTransactionID) transactionID) != null;
            } else if (transactionID instanceof XidTransactionID) {
                try {
                    return getXATerminator().getTransaction(((XidTransactionID) transactionID).getXid()) != null;
                } catch (XAException e) {
                    EjbLogger.ROOT_LOGGER.unexpectedXAException(e);
                    return false;
                }
            }
        }
        return false;
    }
}
