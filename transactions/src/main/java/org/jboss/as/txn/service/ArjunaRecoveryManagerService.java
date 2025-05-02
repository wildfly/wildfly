/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;
import com.arjuna.ats.arjuna.common.recoveryPropertyManager;
import com.arjuna.ats.internal.arjuna.recovery.AtomicActionRecoveryModule;
import com.arjuna.ats.internal.arjuna.recovery.ExpiredTransactionStatusManagerScanner;
import com.arjuna.ats.internal.jta.recovery.arjunacore.CommitMarkableResourceRecordRecoveryModule;
import com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinateAtomicActionRecoveryModule;
import com.arjuna.ats.internal.jts.recovery.contact.ExpiredContactScanner;
import com.arjuna.ats.internal.jta.recovery.jts.JCAServerTransactionRecoveryModule;
import com.arjuna.ats.internal.jts.recovery.transactions.ExpiredServerScanner;
import com.arjuna.ats.internal.jts.recovery.transactions.ExpiredToplevelScanner;
import com.arjuna.ats.internal.jts.recovery.transactions.ServerTransactionRecoveryModule;
import com.arjuna.ats.internal.jts.recovery.transactions.TopLevelTransactionRecoveryModule;
import com.arjuna.ats.internal.txoj.recovery.TORecoveryModule;
import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;
import com.arjuna.orbportability.internal.utils.PostInitLoader;

import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.network.ManagedBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.as.server.suspend.SuspendableActivityRegistry;
import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.as.txn.suspend.RecoverySuspendController;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.omg.CORBA.ORB;

/**
 * A service responsible for exposing the proprietary Arjuna {@link RecoveryManagerService}.
 *
 * @author John Bailey
 * @author Scott Stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class ArjunaRecoveryManagerService implements Service {

    private final Consumer<RecoveryManagerService> consumer;
    private final Supplier<ORB> orbSupplier;
    private final Supplier<SocketBinding> recoveryBindingSupplier;
    private final Supplier<SocketBinding> statusBindingSupplier;
    private final Supplier<SuspendController> suspendControllerSupplier;
    private final Supplier<ProcessStateNotifier> processStateSupplier;

    private RecoveryManagerService recoveryManagerService;
    private RecoverySuspendController recoverySuspendController;
    private boolean recoveryListener;
    private final boolean jts;
    private final Supplier<SocketBindingManager> bindingManagerSupplier;

    public ArjunaRecoveryManagerService(final Consumer<RecoveryManagerService> consumer,
                                        final Supplier<SocketBinding> recoveryBindingSupplier,
                                        final Supplier<SocketBinding> statusBindingSupplier,
                                        final Supplier<SocketBindingManager> bindingManagerSupplier,
                                        final Supplier<SuspendController> suspendControllerSupplier,
                                        final Supplier<ProcessStateNotifier> processStateSupplier,
                                        final Supplier<ORB> orbSupplier,
                                        final boolean recoveryListener, final boolean jts) {
        this.consumer = consumer;
        this.recoveryBindingSupplier = recoveryBindingSupplier;
        this.statusBindingSupplier = statusBindingSupplier;
        this.suspendControllerSupplier = suspendControllerSupplier;
        this.bindingManagerSupplier = bindingManagerSupplier;
        this.processStateSupplier = processStateSupplier;
        this.recoveryListener = recoveryListener;
        this.orbSupplier = orbSupplier;
        this.jts = jts;
    }

    public void start(final StartContext context) throws StartException {

        // Recovery env bean
        final RecoveryEnvironmentBean recoveryEnvironmentBean = recoveryPropertyManager.getRecoveryEnvironmentBean();
        final SocketBinding recoveryBinding = recoveryBindingSupplier.get();
        recoveryEnvironmentBean.setRecoveryInetAddress(recoveryBinding.getSocketAddress().getAddress());
        recoveryEnvironmentBean.setRecoveryPort(recoveryBinding.getSocketAddress().getPort());
        final SocketBinding statusBinding = statusBindingSupplier.get();
        recoveryEnvironmentBean.setTransactionStatusManagerInetAddress(statusBinding.getSocketAddress().getAddress());
        recoveryEnvironmentBean.setTransactionStatusManagerPort(statusBinding.getSocketAddress().getPort());
        recoveryEnvironmentBean.setRecoveryListener(recoveryListener);

        if (recoveryListener){
            ManagedBinding binding = ManagedBinding.Factory.createSimpleManagedBinding(recoveryBinding);
            bindingManagerSupplier.get().getNamedRegistry().registerBinding(binding);
        }

        final List<String> recoveryExtensions = new ArrayList<String>();
        recoveryExtensions.add(CommitMarkableResourceRecordRecoveryModule.class.getName()); // must be first
        recoveryExtensions.add(AtomicActionRecoveryModule.class.getName());
        recoveryExtensions.add(TORecoveryModule.class.getName());
        recoveryExtensions.add(SubordinateAtomicActionRecoveryModule.class.getName());

        final List<String> expiryScanners;
        if (System.getProperty("RecoveryEnvironmentBean.expiryScannerClassNames") != null ||
                System.getProperty("com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean.expiryScannerClassNames") != null) {
            expiryScanners = recoveryEnvironmentBean.getExpiryScannerClassNames();
        } else {
            expiryScanners = new ArrayList<String>();
            expiryScanners.add(ExpiredTransactionStatusManagerScanner.class.getName());
        }


        if (!jts) {
            recoveryExtensions.add(com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule.class.getName());
            recoveryEnvironmentBean.setRecoveryModuleClassNames(recoveryExtensions);
            recoveryEnvironmentBean.setExpiryScannerClassNames(expiryScanners);
            recoveryEnvironmentBean.setRecoveryActivators(null);

            final RecoveryManagerService recoveryManagerService = new RecoveryManagerService();
            try {
                recoveryManagerService.create();
            } catch (Exception e) {
                throw TransactionLogger.ROOT_LOGGER.managerStartFailure(e, "Recovery");
            }

            recoveryManagerService.start();

            this.recoveryManagerService = recoveryManagerService;
        } else {
            final ORB orb = orbSupplier.get();
            new PostInitLoader(PostInitLoader.generateORBPropertyName("com.arjuna.orbportability.orb"), orb);

            recoveryExtensions.add(TopLevelTransactionRecoveryModule.class.getName());
            recoveryExtensions.add(ServerTransactionRecoveryModule.class.getName());
            recoveryExtensions.add(JCAServerTransactionRecoveryModule.class.getName());
            recoveryExtensions.add(com.arjuna.ats.internal.jta.recovery.jts.XARecoveryModule.class.getName());
            expiryScanners.add(ExpiredContactScanner.class.getName());
            expiryScanners.add(ExpiredToplevelScanner.class.getName());
            expiryScanners.add(ExpiredServerScanner.class.getName());
            recoveryEnvironmentBean.setRecoveryModuleClassNames(recoveryExtensions);
            recoveryEnvironmentBean.setExpiryScannerClassNames(expiryScanners);
            recoveryEnvironmentBean.setRecoveryActivatorClassNames(Collections.singletonList(com.arjuna.ats.internal.jts.orbspecific.recovery.RecoveryEnablement.class.getName()));


            try {
                final RecoveryManagerService recoveryManagerService = new com.arjuna.ats.jbossatx.jts.RecoveryManagerService(orb);
                recoveryManagerService.create();
                recoveryManagerService.start();
                this.recoveryManagerService = recoveryManagerService;
            } catch (Exception e) {
                throw TransactionLogger.ROOT_LOGGER.managerStartFailure(e, "Recovery");
            }
        }
        recoverySuspendController = new RecoverySuspendController(recoveryManagerService);
        processStateSupplier.get().addPropertyChangeListener(recoverySuspendController);
        suspendControllerSupplier.get().registerActivity(recoverySuspendController, SuspendableActivityRegistry.SuspendPriority.LAST);
        consumer.accept(recoveryManagerService);
    }

    public void stop(final StopContext context) {
        consumer.accept(null);
        suspendControllerSupplier.get().unregisterActivity(recoverySuspendController);
        processStateSupplier.get().removePropertyChangeListener(recoverySuspendController);
        try {
            recoveryManagerService.stop();
        } catch (Exception e) {
            // todo log
        }
        recoveryManagerService.destroy();
        recoveryManagerService = null;
        recoverySuspendController = null;
    }

}
