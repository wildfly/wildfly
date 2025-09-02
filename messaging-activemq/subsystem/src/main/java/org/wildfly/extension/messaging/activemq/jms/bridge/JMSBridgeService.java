/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms.bridge;

import static org.jboss.as.controller.ModuleIdentifierUtil.parseCanonicalModuleIdentifier;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.activemq.artemis.jms.bridge.JMSBridge;
import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.transaction.client.ContextTransactionManager;

/**
 * Service responsible for Jakarta Messaging Bridges.
 *
 * @author Jeff Mesnil (c) 2012 Red Hat Inc.
 */
class JMSBridgeService implements Service<JMSBridge> {
    private final JMSBridge bridge;
    private final String bridgeName;
    private final String moduleName;
    private final Supplier<ExecutorService> executorSupplier;
    private final ExceptionSupplier<CredentialSource, Exception> sourceCredentialSourceSupplier;
    private final ExceptionSupplier<CredentialSource, Exception> targetCredentialSourceSupplier;

    public JMSBridgeService(final String moduleName, final String bridgeName, final JMSBridge bridge,
            Supplier<ExecutorService> executorSupplier,
            ExceptionSupplier<CredentialSource, Exception> sourceCredentialSourceSupplier,
            ExceptionSupplier<CredentialSource, Exception> targetCredentialSourceSupplier) {
        if(bridge == null) {
            throw MessagingLogger.ROOT_LOGGER.nullVar("bridge");
        }
        this.moduleName = moduleName;
        this.bridgeName = bridgeName;
        this.bridge = bridge;
        this.executorSupplier = executorSupplier;
        this.sourceCredentialSourceSupplier = sourceCredentialSourceSupplier;
        this.targetCredentialSourceSupplier = targetCredentialSourceSupplier;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    bridge.setTransactionManager(ContextTransactionManager.getInstance());
                    startBridge();

                    context.complete();
                } catch (Throwable e) {
                    context.failed(MessagingLogger.ROOT_LOGGER.failedToCreate(e, "JMS Bridge"));
                }
            }
        };
        try {
            executorSupplier.get().execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            context.asynchronous();
        }
    }

    public void startBridge() throws Exception {
        final Module module;
        if (moduleName != null) {
            module =  Module.getContextModuleLoader().loadModule(parseCanonicalModuleIdentifier(moduleName));
        } else {
            module = Module.forClass(JMSBridgeService.class);
        }

        ClassLoader oldTccl= WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(module.getClassLoader());
            setJMSBridgePasswordsFromCredentialSource();
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
            executorSupplier.get().execute(task);
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


    private void setJMSBridgePasswordsFromCredentialSource() {
        setNewJMSBridgePassword(sourceCredentialSourceSupplier, bridge::setSourcePassword);
        setNewJMSBridgePassword(targetCredentialSourceSupplier, bridge::setTargetPassword);
    }

    private void setNewJMSBridgePassword(ExceptionSupplier<CredentialSource, Exception> credentialSourceSupplier, Consumer<String> passwordConsumer) {
        if (credentialSourceSupplier != null) {
            try {
                CredentialSource credentialSource = credentialSourceSupplier.get();
                if (credentialSource != null) {
                    char[] password = credentialSource.getCredential(PasswordCredential.class).getPassword(ClearPassword.class).getPassword();
                    if (password != null) {
                        passwordConsumer.accept(new String(password));
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
