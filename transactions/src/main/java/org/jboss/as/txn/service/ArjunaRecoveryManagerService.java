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

package org.jboss.as.txn.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.network.SocketBinding;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.omg.CORBA.ORB;

import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;
import com.arjuna.ats.arjuna.common.recoveryPropertyManager;
import com.arjuna.ats.internal.arjuna.recovery.AtomicActionRecoveryModule;
import com.arjuna.ats.internal.arjuna.recovery.ExpiredTransactionStatusManagerScanner;
import com.arjuna.ats.internal.jts.recovery.contact.ExpiredContactScanner;
import com.arjuna.ats.internal.jts.recovery.transactions.ExpiredServerScanner;
import com.arjuna.ats.internal.jts.recovery.transactions.ExpiredToplevelScanner;
import com.arjuna.ats.internal.jts.recovery.transactions.ServerTransactionRecoveryModule;
import com.arjuna.ats.internal.jts.recovery.transactions.TopLevelTransactionRecoveryModule;
import com.arjuna.ats.internal.txoj.recovery.TORecoveryModule;
import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;
import com.arjuna.orbportability.internal.utils.PostInitLoader;

import static org.jboss.as.txn.TransactionMessages.MESSAGES;

/**
 * A service responsible for exposing the proprietary Arjuna {@link RecoveryManagerService}.
 *
 * @author John Bailey
 * @author Scott Stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 */
public class ArjunaRecoveryManagerService implements Service<RecoveryManagerService> {

    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER;

    private final InjectedValue<ORB> orbInjector = new InjectedValue<ORB>();
    private final InjectedValue<SocketBinding> recoveryBindingInjector = new InjectedValue<SocketBinding>();
    private final InjectedValue<SocketBinding> statusBindingInjector = new InjectedValue<SocketBinding>();

    private RecoveryManagerService recoveryManagerService;
    private boolean recoveryListener;
    private final boolean jts;

    public ArjunaRecoveryManagerService(final boolean recoveryListener, final boolean jts) {
        this.recoveryListener = recoveryListener;
        this.jts = jts;
    }

    public synchronized void start(StartContext context) throws StartException {

        // Recovery env bean
        final RecoveryEnvironmentBean recoveryEnvironmentBean = recoveryPropertyManager.getRecoveryEnvironmentBean();
        final SocketBinding recoveryBinding = recoveryBindingInjector.getValue();
        recoveryEnvironmentBean.setRecoveryInetAddress(recoveryBinding.getSocketAddress().getAddress());
        recoveryEnvironmentBean.setRecoveryPort(recoveryBinding.getSocketAddress().getPort());
        final SocketBinding statusBinding = statusBindingInjector.getValue();
        recoveryEnvironmentBean.setTransactionStatusManagerInetAddress(statusBinding.getSocketAddress().getAddress());
        recoveryEnvironmentBean.setTransactionStatusManagerPort(statusBinding.getSocketAddress().getPort());
        recoveryEnvironmentBean.setRecoveryListener(recoveryListener);

        final List<String> recoveryExtensions = new ArrayList<String>();
        recoveryExtensions.add(AtomicActionRecoveryModule.class.getName());
        recoveryExtensions.add(TORecoveryModule.class.getName());

        final List<String> expiryScanners = new ArrayList<String>();
        expiryScanners.add(ExpiredTransactionStatusManagerScanner.class.getName());


        if (!jts) {
            recoveryExtensions.add(com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule.class.getName());
            recoveryEnvironmentBean.setRecoveryModuleClassNames(recoveryExtensions);
            recoveryEnvironmentBean.setExpiryScannerClassNames(expiryScanners);
            recoveryEnvironmentBean.setRecoveryActivators(null);

            final RecoveryManagerService recoveryManagerService = new RecoveryManagerService();
            try {
                recoveryManagerService.create();
            } catch (Exception e) {
                throw MESSAGES.managerStartFailure(e, "Recovery");
            }

            recoveryManagerService.start();

            this.recoveryManagerService = recoveryManagerService;
        } else {
            final ORB orb = orbInjector.getValue();
            new PostInitLoader(PostInitLoader.generateORBPropertyName("com.arjuna.orbportability.orb"), orb);

            recoveryExtensions.add(TopLevelTransactionRecoveryModule.class.getName());
            recoveryExtensions.add(ServerTransactionRecoveryModule.class.getName());
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
                throw MESSAGES.managerStartFailure(e, "Recovery");
            }
        }
    }

    public synchronized void stop(StopContext context) {
        try {
            recoveryManagerService.stop();
        } catch (Exception e) {
            // todo log
        }
        recoveryManagerService.destroy();
        recoveryManagerService = null;
    }

    public synchronized RecoveryManagerService getValue() throws IllegalStateException, IllegalArgumentException {
        return recoveryManagerService;
    }

    public Injector<ORB> getOrbInjector() {
        return orbInjector;
    }

    public Injector<SocketBinding> getRecoveryBindingInjector() {
        return recoveryBindingInjector;
    }

    public Injector<SocketBinding> getStatusBindingInjector() {
        return statusBindingInjector;
    }
}
