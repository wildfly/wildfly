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

package org.wildfly.extension.messaging.activemq.jms.bridge;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import javax.transaction.TransactionManager;

import org.apache.activemq.artemis.jms.bridge.JMSBridge;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Service responsible for JMS Bridges.
 *
 * @author Jeff Mesnil (c) 2012 Red Hat Inc.
 */
class JMSBridgeService implements Service<JMSBridge> {
    private final JMSBridge bridge;
    private final String bridgeName;
    private final String moduleName;
    private final InjectedValue<ExecutorService> executorInjector = new InjectedValue<>();

    public JMSBridgeService(final String moduleName, final String bridgeName, final JMSBridge bridge) {
        if(bridge == null) {
            throw MessagingLogger.ROOT_LOGGER.nullVar("bridge");
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
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    bridge.setTransactionManager(getTransactionManager(context));
                    startBridge();

                    context.complete();
                } catch (Throwable e) {
                    context.failed(MessagingLogger.ROOT_LOGGER.failedToCreate(e, "JMS Bridge"));
                }
            }
        };
        try {
            executorInjector.getValue().execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            context.asynchronous();
        }
    }

    public void startBridge() throws Exception {
        final Module module;
        if (moduleName != null) {
            ModuleIdentifier moduleID = ModuleIdentifier.fromString(moduleName);
            module =  Module.getContextModuleLoader().loadModule(moduleID);
        } else {
            module = Module.forClass(JMSBridgeService.class);
        }

        ClassLoader oldTccl= WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(module.getClassLoader());
            bridge.start();
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTccl);
        }
        MessagingLogger.ROOT_LOGGER.startedService("JMS Bridge", bridgeName);
    }

    @Override
    public synchronized void stop(final StopContext context) {
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    bridge.stop();
                    MessagingLogger.ROOT_LOGGER.stoppedService("JMS Bridge", bridgeName);

                    context.complete();
                } catch(Exception e) {
                    MessagingLogger.ROOT_LOGGER.failedToDestroy("JMS Bridge", bridgeName);
                }
            }
        };
        try {
            executorInjector.getValue().execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            context.asynchronous();
        }
    }

    @Override
    public JMSBridge getValue() throws IllegalStateException {
        return bridge;
    }

    public InjectedValue<ExecutorService> getExecutorInjector() {
        return executorInjector;
    }
}
