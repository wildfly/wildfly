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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.transaction.TransactionManager;

import org.jboss.as.services.net.SocketBinding;
import org.jboss.as.services.path.AbstractPathService;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.tm.JBossXATerminator;
import org.jboss.tm.LastResource;
import org.omg.CORBA.ORB;

import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.common.recoveryPropertyManager;
import com.arjuna.ats.arjuna.tools.osb.mbean.ObjStoreBean;
import com.arjuna.ats.internal.arjuna.recovery.AtomicActionRecoveryModule;
import com.arjuna.ats.internal.arjuna.recovery.ExpiredTransactionStatusManagerScanner;
import com.arjuna.ats.internal.jta.recovery.arjunacore.JTANodeNameXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.arjunacore.JTATransactionLogXAResourceOrphanFilter;
import com.arjuna.ats.internal.jts.recovery.contact.ExpiredContactScanner;
import com.arjuna.ats.internal.jts.recovery.transactions.ExpiredServerScanner;
import com.arjuna.ats.internal.jts.recovery.transactions.ExpiredToplevelScanner;
import com.arjuna.ats.internal.jts.recovery.transactions.ServerTransactionRecoveryModule;
import com.arjuna.ats.internal.jts.recovery.transactions.TopLevelTransactionRecoveryModule;
import com.arjuna.ats.internal.txoj.recovery.TORecoveryModule;
import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.ats.jta.common.jtaPropertyManager;
import com.arjuna.common.internal.util.logging.LoggingEnvironmentBean;
import com.arjuna.common.internal.util.logging.commonPropertyManager;
import com.arjuna.common.internal.util.logging.jakarta.JakartaRelevelingLogFactory;
import com.arjuna.common.internal.util.logging.jakarta.Log4JLogger;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class TransactionManagerService implements Service<TransactionManager> {

    private final InjectedValue<JBossXATerminator> xaTerminatorInjector = new InjectedValue<JBossXATerminator>();
    private final InjectedValue<ORB> orbInjector = new InjectedValue<ORB>();

    private final InjectedValue<SocketBinding> recoveryBindingInjector = new InjectedValue<SocketBinding>();
    private final InjectedValue<SocketBinding> statusBindingInjector = new InjectedValue<SocketBinding>();
    private final InjectedValue<SocketBinding> socketProcessBindingInjector = new InjectedValue<SocketBinding>();
    private final InjectedValue<AbstractPathService> pathInjector = new InjectedValue<AbstractPathService>();

    private com.arjuna.ats.jbossatx.jta.TransactionManagerService value;
    private RecoveryManagerService recoveryManagerService;

    private String coreNodeIdentifier;
    private int coreSocketProcessIdMaxPorts;
    private boolean coordinatorEnableStatistics;
    private int coordinatorDefaultTimeout;

    TransactionManagerService(final String coreNodeIdentifier, final int coreSocketProcessIdMaxPorts, final boolean coordinatorEnableStatistics, final int coordinatorDefaultTimeout) {
        this.coreNodeIdentifier = coreNodeIdentifier;
        this.coreSocketProcessIdMaxPorts = coreSocketProcessIdMaxPorts;
        this.coordinatorEnableStatistics = coordinatorEnableStatistics;
        this.coordinatorDefaultTimeout = coordinatorDefaultTimeout;
    }

    public synchronized void start(final StartContext context) throws StartException {
        // JTS expects the TCCL to be set to something that can find the log factory class.
        AccessController.doPrivileged(new SetContextLoaderAction(TransactionManagerService.class.getClassLoader()));
        try {
            // Global configuration.

            // Logging environment config
            final LoggingEnvironmentBean loggingEnvironmentBean = commonPropertyManager.getLoggingEnvironmentBean();
            loggingEnvironmentBean.setLoggingFactory(JakartaRelevelingLogFactory.class.getName() + ";" + Log4JLogger.class.getName());

            // Recovery env bean
            final RecoveryEnvironmentBean recoveryEnvironmentBean = recoveryPropertyManager.getRecoveryEnvironmentBean();
            final SocketBinding recoveryBinding = recoveryBindingInjector.getValue();
            recoveryEnvironmentBean.setRecoveryInetAddress(recoveryBinding.getSocketAddress().getAddress());
            recoveryEnvironmentBean.setRecoveryPort(recoveryBinding.getSocketAddress().getPort());
            final SocketBinding statusBinding = statusBindingInjector.getValue();
            recoveryEnvironmentBean.setTransactionStatusManagerInetAddress(statusBinding.getSocketAddress().getAddress());
            recoveryEnvironmentBean.setTransactionStatusManagerPort(statusBinding.getSocketAddress().getPort());

            final List<String> recoveryExtensions = new ArrayList<String>();
            final List<String> expiryScanners = new ArrayList<String>();

            recoveryExtensions.add(AtomicActionRecoveryModule.class.getName());
            recoveryExtensions.add(TORecoveryModule.class.getName());

            expiryScanners.add(ExpiredTransactionStatusManagerScanner.class.getName());

            final CoreEnvironmentBean coreEnvironmentBean = arjPropertyManager.getCoreEnvironmentBean();
            coreEnvironmentBean.setSocketProcessIdPort(socketProcessBindingInjector.getValue().getSocketAddress().getPort());
            coreEnvironmentBean.setNodeIdentifier(coreNodeIdentifier);
            coreEnvironmentBean.setSocketProcessIdMaxPorts(coreSocketProcessIdMaxPorts);

            final JTAEnvironmentBean jtaEnvironmentBean = jtaPropertyManager.getJTAEnvironmentBean();
            jtaEnvironmentBean.setLastResourceOptimisationInterface(LastResource.class.getName());
            jtaEnvironmentBean.setXaRecoveryNodes(Collections.singletonList("1"));
            jtaEnvironmentBean.setXaResourceOrphanFilterClassNames(Arrays.asList(JTATransactionLogXAResourceOrphanFilter.class.getName(), JTANodeNameXAResourceOrphanFilter.class.getName()));

            final CoordinatorEnvironmentBean coordinatorEnvironmentBean = arjPropertyManager.getCoordinatorEnvironmentBean();
            coordinatorEnvironmentBean.setEnableStatistics(coordinatorEnableStatistics);
            coordinatorEnvironmentBean.setDefaultTimeout(coordinatorDefaultTimeout);

            final ObjectStoreEnvironmentBean objectStoreEnvironmentBean = arjPropertyManager.getObjectStoreEnvironmentBean();
            objectStoreEnvironmentBean.setObjectStoreDir(pathInjector.getValue().getValue());

            try {
                Logger.getLogger("org.jboss.tx").info(ObjStoreBean.getObjectStoreBrowserBean().getStore().storeDir());
            } catch (Exception e) {
                throw new StartException("Failed to configure object store browser bean", e);
            }

            final RecoveryManagerService recoveryManagerService = new RecoveryManagerService();

            final ORB orb = orbInjector.getValue();

            if (orb == null) {
                // No IIOP, stick with JTA mode.
                final com.arjuna.ats.jbossatx.jta.TransactionManagerService service = new com.arjuna.ats.jbossatx.jta.TransactionManagerService();
                service.setJbossXATerminator(xaTerminatorInjector.getValue());
                service.setTransactionSynchronizationRegistry(new com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple());
                recoveryExtensions.add(com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule.class.getName());
                recoveryEnvironmentBean.setRecoveryExtensions(recoveryExtensions);
                recoveryEnvironmentBean.setExpiryScanners(expiryScanners);
                recoveryEnvironmentBean.setRecoveryActivators(null);
                jtaEnvironmentBean.setTransactionManagerClassName(com.arjuna.ats.jbossatx.jta.TransactionManagerDelegate.class.getName());
                jtaEnvironmentBean.setUserTransactionClassName(com.arjuna.ats.internal.jta.transaction.arjunacore.UserTransactionImple.class.getName());
                try {
                    recoveryManagerService.create();
                } catch (Exception e) {
                    throw new StartException("Recovery manager create failed", e);
                }
                recoveryManagerService.start();
                try {
                    service.create();
                } catch (Exception e) {
                    throw new StartException("Transaction manager create failed", e);
                }
                service.start();
                this.recoveryManagerService = recoveryManagerService;
                value = service;
            } else {
                // IIOP is enabled, so fire up JTS mode.
                final com.arjuna.ats.jbossatx.jts.TransactionManagerService service = new com.arjuna.ats.jbossatx.jts.TransactionManagerService();
                service.setJbossXATerminator(xaTerminatorInjector.getValue());
                service.setTransactionSynchronizationRegistry(new com.arjuna.ats.internal.jta.transaction.jts.TransactionSynchronizationRegistryImple());
                recoveryExtensions.add(TopLevelTransactionRecoveryModule.class.getName());
                recoveryExtensions.add(ServerTransactionRecoveryModule.class.getName());
                recoveryExtensions.add(com.arjuna.ats.internal.jta.recovery.jts.XARecoveryModule.class.getName());
                expiryScanners.add(ExpiredContactScanner.class.getName());
                expiryScanners.add(ExpiredToplevelScanner.class.getName());
                expiryScanners.add(ExpiredServerScanner.class.getName());
                recoveryEnvironmentBean.setRecoveryExtensions(recoveryExtensions);
                recoveryEnvironmentBean.setExpiryScanners(expiryScanners);
                recoveryEnvironmentBean.setRecoveryActivators(Collections.singletonList(com.arjuna.ats.internal.jts.orbspecific.recovery.RecoveryEnablement.class.getName()));
                jtaEnvironmentBean.setTransactionManagerClassName(com.arjuna.ats.jbossatx.jts.TransactionManagerDelegate.class.getName());
                jtaEnvironmentBean.setUserTransactionClassName(com.arjuna.ats.internal.jta.transaction.jts.UserTransactionImple.class.getName());

                try {
                    recoveryManagerService.create();
                } catch (Exception e) {
                    throw new StartException("Recovery manager create failed", e);
                }
                recoveryManagerService.start();

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
                this.recoveryManagerService = recoveryManagerService;
                value = service;
            }
            // todo: JNDI bindings
        } finally {
            AccessController.doPrivileged(CLEAR_ACTION);
        }
    }

    public synchronized void stop(final StopContext context) {
        value.stop();
        value.destroy();
        try {
            recoveryManagerService.stop();
        } catch (Exception e) {
            // todo log
        }
        recoveryManagerService.destroy();
        value = null;
        recoveryManagerService = null;
    }

    public synchronized TransactionManager getValue() throws IllegalStateException {
        return TxnServices.notNull(value).getTransactionManager();
    }

    Injector<JBossXATerminator> getXaTerminatorInjector() {
        return xaTerminatorInjector;
    }

    Injector<ORB> getOrbInjector() {
        return orbInjector;
    }

    Injector<SocketBinding> getRecoveryBindingInjector() {
        return recoveryBindingInjector;
    }

    Injector<SocketBinding> getStatusBindingInjector() {
        return statusBindingInjector;
    }

    Injector<SocketBinding> getSocketProcessBindingInjector() {
        return socketProcessBindingInjector;
    }

    InjectedValue<AbstractPathService> getPathInjector() {
        return pathInjector;
    }

    private static final SetContextLoaderAction CLEAR_ACTION = new SetContextLoaderAction(null);

    private static class SetContextLoaderAction implements PrivilegedAction<Void> {

        private final ClassLoader classLoader;

        public SetContextLoaderAction(final ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        public Void run() {
            Thread.currentThread().setContextClassLoader(classLoader);
            return null;
        }
    }
}
