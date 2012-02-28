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

import static org.jboss.as.txn.TransactionMessages.MESSAGES;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.tm.JBossXATerminator;
import org.jboss.tm.LastResource;
import org.jboss.tm.usertx.UserTransactionRegistry;
import org.jboss.tm.usertx.client.ServerVMClientUserTransaction;
import org.omg.CORBA.ORB;

import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.tools.osb.mbean.ObjStoreBrowser;
import com.arjuna.ats.internal.jta.recovery.arjunacore.JTANodeNameXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.arjunacore.JTATransactionLogXAResourceOrphanFilter;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.ats.jta.common.jtaPropertyManager;
import com.arjuna.orbportability.internal.utils.PostInitLoader;

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


    private com.arjuna.ats.jbossatx.jta.TransactionManagerService value;
    private ObjStoreBrowser objStoreBrowser;

    private boolean transactionStatusManagerEnable;
    private boolean coordinatorEnableStatistics;
    private int coordinatorDefaultTimeout;
    private final boolean jts;
    private final String nodeIdentifier;

    public ArjunaTransactionManagerService(final boolean coordinatorEnableStatistics, final int coordinatorDefaultTimeout,
                                    final boolean transactionStatusManagerEnable, final boolean jts, final String nodeIdentifier) {
        this.coordinatorEnableStatistics = coordinatorEnableStatistics;
        this.coordinatorDefaultTimeout = coordinatorDefaultTimeout;
        this.transactionStatusManagerEnable = transactionStatusManagerEnable;
        this.jts = jts;
        this.nodeIdentifier = nodeIdentifier;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {

        final JTAEnvironmentBean jtaEnvironmentBean = jtaPropertyManager.getJTAEnvironmentBean();
        jtaEnvironmentBean.setLastResourceOptimisationInterfaceClassName(LastResource.class.getName());
        jtaEnvironmentBean.setXaRecoveryNodes(Collections.singletonList(nodeIdentifier));
        jtaEnvironmentBean.setXaResourceOrphanFilterClassNames(Arrays.asList(JTATransactionLogXAResourceOrphanFilter.class.getName(), JTANodeNameXAResourceOrphanFilter.class.getName()));
        jtaEnvironmentBean.setXAResourceRecordWrappingPlugin(new com.arjuna.ats.internal.jbossatx.jta.XAResourceRecordWrappingPluginImpl());

        final CoordinatorEnvironmentBean coordinatorEnvironmentBean = arjPropertyManager.getCoordinatorEnvironmentBean();
        coordinatorEnvironmentBean.setEnableStatistics(coordinatorEnableStatistics);
        coordinatorEnvironmentBean.setDefaultTimeout(coordinatorDefaultTimeout);
        coordinatorEnvironmentBean.setTransactionStatusManagerEnable(transactionStatusManagerEnable);

        // Object Store Browser bean
        Map<String, String> objStoreBrowserTypes = new HashMap<String, String>();
        objStoreBrowser = new ObjStoreBrowser();
        objStoreBrowserTypes.put("StateManager/BasicAction/TwoPhaseCoordinator/AtomicAction",
                "com.arjuna.ats.internal.jta.tools.osb.mbean.jta.JTAActionBean");


        if (!jts) {
            // No IIOP, stick with JTA mode.
            jtaEnvironmentBean.setTransactionManagerClassName(com.arjuna.ats.jbossatx.jta.TransactionManagerDelegate.class.getName());
            jtaEnvironmentBean.setTransactionSynchronizationRegistryClassName(com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple.class.getName());

            final com.arjuna.ats.jbossatx.jta.TransactionManagerService service = new com.arjuna.ats.jbossatx.jta.TransactionManagerService();
            final ServerVMClientUserTransaction userTransaction = new ServerVMClientUserTransaction(service.getTransactionManager());
            userTransactionRegistry.getValue().addProvider(userTransaction);
            jtaEnvironmentBean.setUserTransaction(userTransaction);
            service.setJbossXATerminator(xaTerminatorInjector.getValue());
            service.setTransactionSynchronizationRegistry(new com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple());

            try {
                service.create();
            } catch (Exception e) {
                throw MESSAGES.managerStartFailure(e, "Transaction");
            }
            service.start();
            value = service;
        } else {
            final ORB orb = orbInjector.getValue();
            new PostInitLoader(PostInitLoader.generateORBPropertyName("com.arjuna.orbportability.orb"), orb);

            // IIOP is enabled, so fire up JTS mode.
            jtaEnvironmentBean.setTransactionManagerClassName(com.arjuna.ats.jbossatx.jts.TransactionManagerDelegate.class.getName());
            jtaEnvironmentBean.setTransactionSynchronizationRegistryClassName(com.arjuna.ats.internal.jta.transaction.jts.TransactionSynchronizationRegistryImple.class.getName());

            final com.arjuna.ats.jbossatx.jts.TransactionManagerService service = new com.arjuna.ats.jbossatx.jts.TransactionManagerService();
            final ServerVMClientUserTransaction userTransaction = new ServerVMClientUserTransaction(service.getTransactionManager());
            jtaEnvironmentBean.setUserTransaction(userTransaction);
            userTransactionRegistry.getValue().addProvider(userTransaction);
            service.setJbossXATerminator(xaTerminatorInjector.getValue());
            service.setTransactionSynchronizationRegistry(new com.arjuna.ats.internal.jta.transaction.jts.TransactionSynchronizationRegistryImple());

            objStoreBrowserTypes.put("StateManager/BasicAction/TwoPhaseCoordinator/ArjunaTransactionImple",
                    "com.arjuna.ats.arjuna.tools.osb.mbean.ActionBean");

            try {
                service.create();
            } catch (Exception e) {
                throw MESSAGES.createFailed(e);
            }
            try {
                service.start(orb);
            } catch (Exception e) {
                throw MESSAGES.startFailure(e);
            }
            value = service;
        }

        try {
            objStoreBrowser.start();
        } catch (Exception e) {
            throw MESSAGES.objectStoreStartFailure(e);
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
}
