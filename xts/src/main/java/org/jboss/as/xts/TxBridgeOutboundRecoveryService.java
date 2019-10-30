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
package org.jboss.as.xts;

import org.jboss.as.xts.logging.XtsAsLogger;
import org.jboss.jbossts.txbridge.outbound.OutboundBridgeRecoveryManager;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.StartException;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * JTA / WS-AT transaction bridge - outbound recovery handling.
 *
 * @author <a href="mailto:jonathan.halliday@redhat.com">Jonathan Halliday</a>
 */
public class TxBridgeOutboundRecoveryService extends AbstractService<OutboundBridgeRecoveryManager> {

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
