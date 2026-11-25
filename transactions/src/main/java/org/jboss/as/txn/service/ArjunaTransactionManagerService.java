/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.coordinator.TxControl;
import com.arjuna.ats.arjuna.tools.osb.mbean.ObjStoreBrowser;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.orbportability.internal.utils.PostInitLoader;

import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.as.txn.service.internal.tsr.TransactionSynchronizationRegistryWrapper;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.tm.JBossXATerminator;
import org.jboss.tm.TransactionManagerLocator;
import org.jboss.tm.usertx.UserTransactionRegistry;
import org.omg.CORBA.ORB;
import org.wildfly.transaction.client.LocalUserTransaction;

/**
 * A service for the proprietary Arjuna {@link com.arjuna.ats.jbossatx.jta.TransactionManagerService}
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Thomas.Diesler@jboss.com
 * @author Scott Stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ArjunaTransactionManagerService implements Service {

    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN_ARJUNA_TRANSACTION_MANAGER;
    private final Consumer<com.arjuna.ats.jbossatx.jta.TransactionManagerService> txnManagerServiceConsumer;
    private final Supplier<JBossXATerminator> xaTerminatorSupplier;
    private final Supplier<ORB> orbSupplier;
    private final Supplier<UserTransactionRegistry> userTransactionRegistrySupplier;
    private final Supplier<JTAEnvironmentBean> jtaEnvironmentBeanSupplier;
    private com.arjuna.ats.jbossatx.jta.TransactionManagerService value;
    private ObjStoreBrowser objStoreBrowser;
    private boolean transactionStatusManagerEnable;
    private boolean coordinatorEnableStatistics;
    private int coordinatorDefaultTimeout;
    private final boolean jts;

    public ArjunaTransactionManagerService(final Consumer<com.arjuna.ats.jbossatx.jta.TransactionManagerService> txnManagerServiceConsumer,
                                           final Supplier<JBossXATerminator> xaTerminatorSupplier,
                                           final Supplier<ORB> orbSupplier,
                                           final Supplier<UserTransactionRegistry> userTransactionRegistrySupplier,
                                           final Supplier<JTAEnvironmentBean> jtaEnvironmentBeanSupplier,
                                           final boolean coordinatorEnableStatistics, final int coordinatorDefaultTimeout,
                                           final boolean transactionStatusManagerEnable, final boolean jts) {
        this.txnManagerServiceConsumer = txnManagerServiceConsumer;
        this.xaTerminatorSupplier = xaTerminatorSupplier;
        this.orbSupplier = orbSupplier;
        this.userTransactionRegistrySupplier = userTransactionRegistrySupplier;
        this.jtaEnvironmentBeanSupplier = jtaEnvironmentBeanSupplier;
        this.coordinatorEnableStatistics = coordinatorEnableStatistics;
        this.coordinatorDefaultTimeout = coordinatorDefaultTimeout;
        this.transactionStatusManagerEnable = transactionStatusManagerEnable;
        this.jts = jts;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {

        final CoordinatorEnvironmentBean coordinatorEnvironmentBean = arjPropertyManager.getCoordinatorEnvironmentBean();
        coordinatorEnvironmentBean.setEnableStatistics(coordinatorEnableStatistics);
        coordinatorEnvironmentBean.setDefaultTimeout(coordinatorDefaultTimeout);
        coordinatorEnvironmentBean.setTransactionStatusManagerEnable(transactionStatusManagerEnable);

        TxControl.setDefaultTimeout(coordinatorDefaultTimeout);

        // Object Store Browser bean
        objStoreBrowser = ObjStoreBrowser.getInstance();

        if (!jts) {
            // No IIOP, stick with Jakarta Transactions mode.
            final com.arjuna.ats.jbossatx.jta.TransactionManagerService service = new com.arjuna.ats.jbossatx.jta.TransactionManagerService();
            final LocalUserTransaction userTransaction = LocalUserTransaction.getInstance();
            jtaEnvironmentBeanSupplier.get().setUserTransaction(userTransaction);
            service.setJbossXATerminator(xaTerminatorSupplier.get());
            service.setTransactionSynchronizationRegistry(new TransactionSynchronizationRegistryWrapper());

            try {
                service.create();
            } catch (Exception e) {
                throw TransactionLogger.ROOT_LOGGER.managerStartFailure(e, "Transaction");
            }
            service.start();
            value = service;
        } else {
            final ORB orb = orbSupplier.get();
            new PostInitLoader(PostInitLoader.generateORBPropertyName("com.arjuna.orbportability.orb"), orb);

            // IIOP is enabled, so fire up JTS mode.
            final com.arjuna.ats.jbossatx.jts.TransactionManagerService service = new com.arjuna.ats.jbossatx.jts.TransactionManagerService();
            final LocalUserTransaction userTransaction = LocalUserTransaction.getInstance();
            jtaEnvironmentBeanSupplier.get().setUserTransaction(userTransaction);
            service.setJbossXATerminator(xaTerminatorSupplier.get());
            service.setTransactionSynchronizationRegistry(new TransactionSynchronizationRegistryWrapper());
            service.setPropagateFullContext(true);

            // this is not great, but it's the only way presently to influence the behavior of com.arjuna.ats.internal.jbossatx.jts.InboundTransactionCurrentImple
            try {
                final Field field = TransactionManagerLocator.class.getDeclaredField("tm");
                field.setAccessible(true);
                field.set(TransactionManagerLocator.getInstance(), jtaEnvironmentBeanSupplier.get().getTransactionManager());
            } catch (IllegalAccessException e) {
                throw new IllegalAccessError(e.getMessage());
            } catch (NoSuchFieldException e) {
                throw new NoSuchFieldError(e.getMessage());
            }

            try {
                service.create();
            } catch (Exception e) {
                throw TransactionLogger.ROOT_LOGGER.createFailed(e);
            }
            try {
                service.start(orb);
            } catch (Exception e) {
                throw TransactionLogger.ROOT_LOGGER.startFailure(e);
            }
            value = service;
        }

        try {
            objStoreBrowser.start();
        } catch (Exception e) {
            throw TransactionLogger.ROOT_LOGGER.objectStoreStartFailure(e);
        }
        txnManagerServiceConsumer.accept(value);
    }

    @Override
    public void stop(final StopContext context) {
        txnManagerServiceConsumer.accept(null);
        value.stop();
        value.destroy();
        objStoreBrowser.stop();
        value = null;
    }
}
