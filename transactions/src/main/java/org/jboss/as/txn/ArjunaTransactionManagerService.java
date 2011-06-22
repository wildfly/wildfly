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

package org.jboss.as.txn;

import static org.jboss.as.txn.SecurityActions.setContextLoader;

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
import org.omg.CORBA.ORB;

import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.tools.osb.mbean.ObjStoreBrowser;
import com.arjuna.ats.internal.jta.recovery.arjunacore.JTANodeNameXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.arjunacore.JTATransactionLogXAResourceOrphanFilter;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.ats.jta.common.jtaPropertyManager;

/**
 * A service for the propriatary Arjuna {@link com.arjuna.ats.jbossatx.jta.TransactionManagerService}
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Thomas.Diesler@jboss.com
 * @author Scott Stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 */
final class ArjunaTransactionManagerService implements Service<com.arjuna.ats.jbossatx.jta.TransactionManagerService> {

    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN_ARJUNA_TRANSACTION_MANAGER;

    private final InjectedValue<JBossXATerminator> xaTerminatorInjector = new InjectedValue<JBossXATerminator>();
    private final InjectedValue<ORB> orbInjector = new InjectedValue<ORB>();


    private com.arjuna.ats.jbossatx.jta.TransactionManagerService value;
    private ObjStoreBrowser objStoreBrowser;

    private boolean transactionStatusManagerEnable;
    private boolean coordinatorEnableStatistics;
    private int coordinatorDefaultTimeout;

    ArjunaTransactionManagerService(final boolean coordinatorEnableStatistics, final int coordinatorDefaultTimeout,
                                    final boolean transactionStatusManagerEnable) {
        this.coordinatorEnableStatistics = coordinatorEnableStatistics;
        this.coordinatorDefaultTimeout = coordinatorDefaultTimeout;
        this.transactionStatusManagerEnable = transactionStatusManagerEnable;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {

        // JTS expects the TCCL to be set to something that can find the log factory class.
        setContextLoader(ArjunaTransactionManagerService.class.getClassLoader());

        try {

            final JTAEnvironmentBean jtaEnvironmentBean = jtaPropertyManager.getJTAEnvironmentBean();
            jtaEnvironmentBean.setLastResourceOptimisationInterfaceClassName(LastResource.class.getName());
            jtaEnvironmentBean.setXaRecoveryNodes(Collections.singletonList("1"));
            jtaEnvironmentBean.setXaResourceOrphanFilterClassNames(Arrays.asList(JTATransactionLogXAResourceOrphanFilter.class.getName(), JTANodeNameXAResourceOrphanFilter.class.getName()));

            final CoordinatorEnvironmentBean coordinatorEnvironmentBean = arjPropertyManager.getCoordinatorEnvironmentBean();
            coordinatorEnvironmentBean.setEnableStatistics(coordinatorEnableStatistics);
            coordinatorEnvironmentBean.setDefaultTimeout(coordinatorDefaultTimeout);
            coordinatorEnvironmentBean.setTransactionStatusManagerEnable(transactionStatusManagerEnable);

            // Object Store Browser bean
            Map<String, String> objStoreBrowserTypes = new HashMap<String, String> ();
               objStoreBrowser = new ObjStoreBrowser();
            objStoreBrowserTypes.put("StateManager/BasicAction/TwoPhaseCoordinator/AtomicAction",
                "com.arjuna.ats.internal.jta.tools.osb.mbean.jta.JTAActionBean");

            final ORB orb = orbInjector.getOptionalValue();

            if (orb == null) {
                // No IIOP, stick with JTA mode.
                final com.arjuna.ats.jbossatx.jta.TransactionManagerService service = new com.arjuna.ats.jbossatx.jta.TransactionManagerService();
                service.setJbossXATerminator(xaTerminatorInjector.getValue());
                service.setTransactionSynchronizationRegistry(new com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple());

                jtaEnvironmentBean.setTransactionManagerClassName(com.arjuna.ats.jbossatx.jta.TransactionManagerDelegate.class.getName());
                jtaEnvironmentBean.setUserTransactionClassName(com.arjuna.ats.internal.jta.transaction.arjunacore.UserTransactionImple.class.getName());
                jtaEnvironmentBean.setTransactionSynchronizationRegistryClassName(com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple.class.getName());

                try {
                    service.create();
                } catch (Exception e) {
                    throw new StartException("Transaction manager create failed", e);
                }
                service.start();
                value = service;
            } else {
                // IIOP is enabled, so fire up JTS mode.
                final com.arjuna.ats.jbossatx.jts.TransactionManagerService service = new com.arjuna.ats.jbossatx.jts.TransactionManagerService();
                service.setJbossXATerminator(xaTerminatorInjector.getValue());
                service.setTransactionSynchronizationRegistry(new com.arjuna.ats.internal.jta.transaction.jts.TransactionSynchronizationRegistryImple());

                jtaEnvironmentBean.setTransactionManagerClassName(com.arjuna.ats.jbossatx.jts.TransactionManagerDelegate.class.getName());
                jtaEnvironmentBean.setUserTransactionClassName(com.arjuna.ats.internal.jta.transaction.jts.UserTransactionImple.class.getName());
                jtaEnvironmentBean.setTransactionSynchronizationRegistryClassName(com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple.class.getName());
                objStoreBrowserTypes.put("StateManager/BasicAction/TwoPhaseCoordinator/ArjunaTransactionImple",
                    "com.arjuna.ats.arjuna.tools.osb.mbean.ActionBean");

                try {
                    service.create();
                } catch (Exception e) {
                    throw new StartException("Create failed", e);
                }
                try {
                    service.start(orb);
                } catch (Exception e) {
                    throw new StartException("Start failed", e);
                }
                value = service;
            }

            try {
                objStoreBrowser.setTypes(objStoreBrowserTypes);
                objStoreBrowser.start();
            } catch (Exception e) {
                throw new StartException("Failed to configure object store browser bean", e);
            }
            // todo: JNDI bindings
        } finally {
            setContextLoader(null);
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
        setContextLoader(ArjunaTransactionManagerService.class.getClassLoader());
        try {
            return TxnServices.notNull(value);
        } finally {
            setContextLoader(null);
        }
    }

    Injector<JBossXATerminator> getXaTerminatorInjector() {
        return xaTerminatorInjector;
    }

    Injector<ORB> getOrbInjector() {
        return orbInjector;
    }

}
