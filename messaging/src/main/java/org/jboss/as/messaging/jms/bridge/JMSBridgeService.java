/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.jms.bridge;

import static org.jboss.as.messaging.MessagingLogger.MESSAGING_LOGGER;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.concurrent.ExecutorService;

import javax.transaction.TransactionManager;

import org.hornetq.jms.bridge.JMSBridge;
import org.jboss.as.messaging.MessagingLogger;
import org.jboss.as.messaging.jms.SecurityActions;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service responsible for JMS Bridges.
 *
 * @author Jeff Mesnil (c) 2012 Red Hat Inc.
 */
class JMSBridgeService implements Service<JMSBridge> {
    private final JMSBridge bridge;
    private final String bridgeName;
    private final String moduleName;
    private final InjectedValue<ExecutorService> executorInjector = new InjectedValue<ExecutorService>();

    public JMSBridgeService(final String moduleName, final String bridgeName, final JMSBridge bridge) {
        if(bridge == null) {
            throw MESSAGES.nullVar("bridge");
        }
        this.moduleName = moduleName;
        this.bridgeName = bridgeName;
        this.bridge = bridge;
    }

    public static TransactionManager getTransactionManager(StartContext context) {
        @SuppressWarnings("unchecked")
        ServiceController<TransactionManager> service = (ServiceController<TransactionManager>) context.getController().getServiceContainer().getService(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER);
        return service == null ? null : service.getValue();
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    bridge.setTransactionManager(getTransactionManager(context));
                    startBridge();

                    context.complete();
                } catch (Throwable e) {
                    context.failed(MESSAGES.failedToCreate(e, "JMS Bridge"));
                }
            }
        };
        context.asynchronous();
        executorInjector.getValue().execute(r);
    }

    public void startBridge() throws Exception {
        if (moduleName == null) {
            bridge.start();
        } else {
            ClassLoader cl = SecurityActions.getContextClassLoader();
            try {
                ModuleIdentifier moduleID = ModuleIdentifier.create(moduleName);
                Module module = Module.getCallerModuleLoader().loadModule(moduleID);
                SecurityActions.setContextClassLoader(module.getClassLoader());
                bridge.start();
            } finally {
                SecurityActions.setContextClassLoader(cl);
            }
        }
        MessagingLogger.MESSAGING_LOGGER.startedService("JMS Bridge", bridgeName);
    }

    @Override
    public synchronized void stop(final StopContext context) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    bridge.stop();
                    MessagingLogger.MESSAGING_LOGGER.stoppedService("JMS Bridge", bridgeName);

                    context.complete();
                } catch(Exception e) {
                    MESSAGING_LOGGER.failedToDestroy("bridge", bridgeName);
                }
            }
        };
        context.asynchronous();
        executorInjector.getValue().execute(r);
    }

    @Override
    public JMSBridge getValue() throws IllegalStateException {
        return bridge;
    }

    public InjectedValue<ExecutorService> getExecutorInjector() {
        return executorInjector;
    }
}
