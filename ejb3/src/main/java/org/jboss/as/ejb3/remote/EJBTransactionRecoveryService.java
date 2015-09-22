/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import javax.transaction.xa.XAResource;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.tm.XAResourceRecovery;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;

/**
 *  TODO wildfly-transaction-client integration
 * Responsible for handing out the {@link #getXAResources() EJB XAResource(s)} during transaction recovery
 *
 * @author Jaikiran Pai
 */
public class EJBTransactionRecoveryService implements Service<EJBTransactionRecoveryService>, XAResourceRecovery {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb").append("tx-recovery-service");
    public static final EJBTransactionRecoveryService INSTANCE = new EJBTransactionRecoveryService();


    private final InjectedValue<RecoveryManagerService> recoveryManagerService = new InjectedValue<RecoveryManagerService>();
    private final InjectedValue<CoreEnvironmentBean> arjunaTxCoreEnvironmentBean = new InjectedValue<CoreEnvironmentBean>();
    private final InjectedValue<ExecutorService> executor = new InjectedValue<ExecutorService>();

    private EJBTransactionRecoveryService() {
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        // register ourselves to the recovery manager service
        recoveryManagerService.getValue().addXAResourceRecovery(this);
        EjbLogger.REMOTE_LOGGER.debugf("Registered %s with the transaction recovery manager", this);
    }

    @Override
    public void stop(final StopContext stopContext) {
        final ExecutorService executorService = executor.getValue();
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    // un-register ourselves from the recovery manager service
                    recoveryManagerService.getValue().removeXAResourceRecovery(EJBTransactionRecoveryService.this);
                    EjbLogger.REMOTE_LOGGER.debugf("Un-registered %s from the transaction recovery manager", this);
                } finally {
                    stopContext.complete();
                }
            }
        };
        try {
            executorService.execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            stopContext.asynchronous();
        }
    }

    @Override
    public EJBTransactionRecoveryService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public XAResource[] getXAResources() {
        return new XAResource[0];
    }

    public Injector<RecoveryManagerService> getRecoveryManagerServiceInjector() {
        return this.recoveryManagerService;
    }

    public Injector<CoreEnvironmentBean> getCoreEnvironmentBeanInjector() {
        return this.arjunaTxCoreEnvironmentBean;
    }

    public InjectedValue<ExecutorService> getExecutorInjector() {
        return executor;
    }


}
