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

import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;
import com.arjuna.ats.arjuna.common.recoveryPropertyManager;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;
import com.arjuna.common.internal.util.logging.commonPropertyManager;
import java.util.ArrayList;
import java.util.List;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.tm.JBossXATerminator;
import org.omg.CORBA.ORB;

import javax.transaction.TransactionManager;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class TransactionManagerService implements Service<TransactionManager> {

    private final InjectedValue<JBossXATerminator> xaTerminatorInjector = new InjectedValue<JBossXATerminator>();
    private final InjectedValue<TransactionSynchronizationRegistryImple> registryInjector = new InjectedValue<TransactionSynchronizationRegistryImple>();
    private final InjectedValue<ORB> orbInjector = new InjectedValue<ORB>();

    private volatile com.arjuna.ats.jbossatx.jta.TransactionManagerService value;

    public synchronized void start(final StartContext context) throws StartException {
        // Global configuration.

        // Logging environment config
        commonPropertyManager.getLoggingEnvironmentBean().setLoggingFactory("com.arjuna.common.internal.util.logging.jakarta.JakartaRelevelingLogFactory;com.arjuna.common.internal.util.logging.jakarta.Log4JLogger");

        // Recovery env bean
        final RecoveryEnvironmentBean recoveryEnvironmentBean = recoveryPropertyManager.getRecoveryEnvironmentBean();
        recoveryEnvironmentBean.setRecoveryInetAddress(null); // todo - service binding
        recoveryEnvironmentBean.setRecoveryPort(0); // todo - service binding
        recoveryEnvironmentBean.setTransactionStatusManagerInetAddress(null); // todo - service binding
        recoveryEnvironmentBean.setTransactionStatusManagerPort(0); // todo - service binding
        final List<String> recoveryExtensions = new ArrayList<String>();


        final ORB orb = orbInjector.getValue();

        if (orb != null) {
            // IIOP is enabled, so fire up JTS mode.
            final com.arjuna.ats.jbossatx.jts.TransactionManagerService service = new com.arjuna.ats.jbossatx.jts.TransactionManagerService();
            service.setJbossXATerminator(xaTerminatorInjector.getValue());
            service.setTransactionSynchronizationRegistry(registryInjector.getValue());
            recoveryExtensions.add("com.arjuna.ats.internal.arjuna.recovery.AtomicActionRecoveryModule");
            recoveryExtensions.add("com.arjuna.ats.internal.txoj.recovery.TORecoveryModule");
            recoveryExtensions.add("com.arjuna.ats.internal.jts.recovery.transactions.TopLevelTransactionRecoveryModule");
            recoveryExtensions.add("com.arjuna.ats.internal.jts.recovery.transactions.ServerTransactionRecoveryModule");
            recoveryExtensions.add("com.arjuna.ats.internal.jta.recovery.jts.XARecoveryModule");
            recoveryEnvironmentBean.setRecoveryExtensions(recoveryExtensions);
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
        } else {
            // No IIOP, stick with JTA mode.
            final com.arjuna.ats.jbossatx.jta.TransactionManagerService service = new com.arjuna.ats.jbossatx.jta.TransactionManagerService();
            service.setJbossXATerminator(xaTerminatorInjector.getValue());
            service.setTransactionSynchronizationRegistry(registryInjector.getValue());
            recoveryExtensions.add("com.arjuna.ats.internal.arjuna.recovery.AtomicActionRecoveryModule");
            recoveryExtensions.add("com.arjuna.ats.internal.txoj.recovery.TORecoveryModule");
            recoveryExtensions.add("com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule");
            recoveryEnvironmentBean.setRecoveryExtensions(recoveryExtensions);
            try {
                service.create();
            } catch (Exception e) {
                throw new StartException("Create failed", e);
            }
            service.start();
            value = service;
        }
    }

    public synchronized void stop(final StopContext context) {
        value.stop();
        value.destroy();
        value = null;
    }

    public synchronized TransactionManager getValue() throws IllegalStateException {
        return TxnServices.notNull(value).getTransactionManager();
    }

    Injector<JBossXATerminator> getXaTerminatorInjector() {
        return xaTerminatorInjector;
    }

    Injector<TransactionSynchronizationRegistryImple> getRegistryInjector() {
        return registryInjector;
    }

    Injector<ORB> getOrbInjector() {
        return orbInjector;
    }
}
