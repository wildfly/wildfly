/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.xts;

import org.jboss.as.xts.logging.XtsAsLogger;
import org.jboss.jbossts.txbridge.outbound.OutboundBridgeRecoveryManager;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.StartException;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Jakarta Transactions / WS-AT transaction bridge - outbound recovery handling.
 *
 * @author <a href="mailto:jonathan.halliday@redhat.com">Jonathan Halliday</a>
 */
public class TxBridgeOutboundRecoveryService implements Service<OutboundBridgeRecoveryManager> {

    private OutboundBridgeRecoveryManager outboundBridgeRecoveryManager;

    public TxBridgeOutboundRecoveryService() {

    }

    @Override
    public synchronized OutboundBridgeRecoveryManager getValue() throws IllegalStateException {
        return outboundBridgeRecoveryManager;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {

        // XTS expects the TCCL to be set to something that will locate the XTS service implementation classes.
        final ClassLoader loader = TxBridgeOutboundRecoveryService.class.getClassLoader();
        WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(loader);
        try {

            OutboundBridgeRecoveryManager service = new OutboundBridgeRecoveryManager();

            try {
                service.start();
            } catch (Exception e) {
                throw XtsAsLogger.ROOT_LOGGER.txBridgeOutboundRecoveryServiceFailedToStart();
            }
            outboundBridgeRecoveryManager = service;
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged((ClassLoader) null);
        }
    }

    public synchronized void stop(final StopContext context) {
        if (outboundBridgeRecoveryManager != null) {
            try {
                outboundBridgeRecoveryManager.stop();
            } catch (Exception e) {
                // ignore?
            }
        }
    }
}
