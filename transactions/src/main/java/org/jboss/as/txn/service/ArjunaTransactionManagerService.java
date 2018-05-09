/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.txn.service;

import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.tools.osb.mbean.ObjStoreBrowser;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.orbportability.internal.utils.PostInitLoader;
import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.as.txn.service.internal.tsr.TransactionSynchronizationRegistryWrapper;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.tm.JBossXATerminator;
import org.jboss.tm.TransactionManagerLocator;
import org.jboss.tm.usertx.UserTransactionRegistry;
import org.omg.CORBA.ORB;
import org.wildfly.transaction.client.LocalUserTransaction;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * A service for the proprietary Arjuna {@link com.arjuna.ats.jbossatx.jta.TransactionManagerService}
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Thomas.Diesler@jboss.com
 * @author Scott Stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 */
public final class ArjunaTransactionManagerService implements Service<com.arjuna.ats.jbossatx.jta.TransactionManagerService> {

    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN_ARJUNA_TRANSACTION_MANAGER;

    private final InjectedValue<JBossXATerminator> xaTerminatorInjector = new InjectedValue<JBossXATerminator>();
    private final InjectedValue<ORB> orbInjector = new InjectedValue<ORB>();
    private final InjectedValue<UserTransactionRegistry> userTransactionRegistry = new InjectedValue<UserTransactionRegistry>();
    private final InjectedValue<JTAEnvironmentBean> jtaEnvironmentBean = new InjectedValue<>();


    private com.arjuna.ats.jbossatx.jta.TransactionManagerService value;
    private ObjStoreBrowser objStoreBrowser;

    private boolean transactionStatusManagerEnable;
    private boolean coordinatorEnableStatistics;
    private int coordinatorDefaultTimeout;
    private final boolean jts;

    public ArjunaTransactionManagerService(final boolean coordinatorEnableStatistics, final int coordinatorDefaultTimeout,
                                           final boolean transactionStatusManagerEnable, final boolean jts) {
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

        // Object Store Browser bean
        Map<String, String> objStoreBrowserTypes = new HashMap<String, String>();
        objStoreBrowser = new ObjStoreBrowser();
        objStoreBrowserTypes.put("StateManager/BasicAction/TwoPhaseCoordinator/AtomicAction",
                "com.arjuna.ats.internal.jta.tools.osb.mbean.jta.JTAActionBean");
        objStoreBrowserTypes.put("StateManager/AbstractRecord/ConnectableResourceRecord",
                "com.arjuna.ats.internal.jta.tools.osb.mbean.jta.ConnectableResourceRecordBean");


        if (!jts) {
            // No IIOP, stick with JTA mode.
            final com.arjuna.ats.jbossatx.jta.TransactionManagerService service = new com.arjuna.ats.jbossatx.jta.TransactionManagerService();
            final LocalUserTransaction userTransaction = LocalUserTransaction.getInstance();
            jtaEnvironmentBean.getValue().setUserTransaction(userTransaction);
            service.setJbossXATerminator(xaTerminatorInjector.getValue());
            service.setTransactionSynchronizationRegistry(new TransactionSynchronizationRegistryWrapper());

            try {
                service.create();
            } catch (Exception e) {
                throw TransactionLogger.ROOT_LOGGER.managerStartFailure(e, "Transaction");
            }
            service.start();
            value = service;
        } else {
            final ORB orb = orbInjector.getValue();
            new PostInitLoader(PostInitLoader.generateORBPropertyName("com.arjuna.orbportability.orb"), orb);

            // IIOP is enabled, so fire up JTS mode.
            final com.arjuna.ats.jbossatx.jts.TransactionManagerService service = new com.arjuna.ats.jbossatx.jts.TransactionManagerService();
            final LocalUserTransaction userTransaction = LocalUserTransaction.getInstance();
            jtaEnvironmentBean.getValue().setUserTransaction(userTransaction);
            service.setJbossXATerminator(xaTerminatorInjector.getValue());
            service.setTransactionSynchronizationRegistry(new TransactionSynchronizationRegistryWrapper());
            service.setPropagateFullContext(true);

            // this is not great, but it's the only way presently to influence the behavior of com.arjuna.ats.internal.jbossatx.jts.InboundTransactionCurrentImple
            try {
                final Field field = TransactionManagerLocator.class.getDeclaredField("tm");
                field.setAccessible(true);
                field.set(TransactionManagerLocator.getInstance(), jtaEnvironmentBean.getValue().getTransactionManager());
            } catch (IllegalAccessException e) {
                throw new IllegalAccessError(e.getMessage());
            } catch (NoSuchFieldException e) {
                throw new NoSuchFieldError(e.getMessage());
            }

            objStoreBrowserTypes.put("StateManager/BasicAction/TwoPhaseCoordinator/ArjunaTransactionImple",
                    "com.arjuna.ats.arjuna.tools.osb.mbean.ActionBean");

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

    }

    @Override
    public synchronized void stop(final StopContext context) {
        value.stop();
        value.destroy();
        objStoreBrowser.stop();
        value = null;
    }

    @Override
    public synchronized com.arjuna.ats.jbossatx.jta.TransactionManagerService getValue() throws IllegalStateException {
        return TxnServices.notNull(value);
    }

    public Injector<JBossXATerminator> getXaTerminatorInjector() {
        return xaTerminatorInjector;
    }

    public Injector<ORB> getOrbInjector() {
        return orbInjector;
    }

    public InjectedValue<UserTransactionRegistry> getUserTransactionRegistry() {
        return userTransactionRegistry;
    }

    public Injector<JTAEnvironmentBean> getJTAEnvironmentBeanInjector() {
        return this.jtaEnvironmentBean;
    }
}
