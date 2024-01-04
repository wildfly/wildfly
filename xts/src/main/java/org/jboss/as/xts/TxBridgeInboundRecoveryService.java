/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.xts;

import org.jboss.as.xts.logging.XtsAsLogger;
import org.jboss.jbossts.txbridge.inbound.InboundBridgeRecoveryManager;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.StartException;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Jakarta Transactions / WS-AT transaction bridge - inbound recovery handling.
 *
 * @author <a href="mailto:jonathan.halliday@redhat.com">Jonathan Halliday</a>
 */
public class TxBridgeInboundRecoveryService implements Service<InboundBridgeRecoveryManager> {

    private volatile InboundBridgeRecoveryManager inboundBridgeRecoveryManager;

    public TxBridgeInboundRecoveryService() {

    }

    @Override
    public InboundBridgeRecoveryManager getValue() throws IllegalStateException {
        return inboundBridgeRecoveryManager;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {

        // XTS expects the TCCL to be set to something that will locate the XTS service implementation classes.
        final ClassLoader loader = TxBridgeInboundRecoveryService.class.getClassLoader();
        WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(loader);
        try {

            InboundBridgeRecoveryManager service = new InboundBridgeRecoveryManager();

            try {
                service.start();
            } catch (Exception e) {
                throw XtsAsLogger.ROOT_LOGGER.txBridgeInboundRecoveryServiceFailedToStart();
            }
            inboundBridgeRecoveryManager = service;
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged((ClassLoader) null);
        }
    }

    public synchronized void stop(final StopContext context) {
        if (inboundBridgeRecoveryManager != null) {
            try {
                inboundBridgeRecoveryManager.stop();
            } catch (Exception e) {
                // ignore?
            }
        }
    }
}
