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
package org.wildfly.extension.rts.service;

import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.arjuna.recovery.RecoveryModule;
import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
import com.arjuna.ats.jta.recovery.XAResourceOrphanFilter;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.narayana.rest.bridge.inbound.InboundBridge;
import org.jboss.narayana.rest.bridge.inbound.InboundBridgeOrphanFilter;
import org.jboss.narayana.rest.bridge.inbound.InboundBridgeRecoveryModule;
import org.wildfly.extension.rts.logging.RTSLogger;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class InboundBridgeService implements Service<InboundBridgeService> {

    private RecoveryModule recoveryModule;

    private XAResourceOrphanFilter orphanFilter;

    @Override
    public void start(StartContext startContext) throws StartException {
        RTSLogger.ROOT_LOGGER.trace("InboundBridgeService.start");

        addDeserializerAndOrphanFilter();
        addRecoveryModule();
    }

    @Override
    public void stop(StopContext stopContext) {
        RTSLogger.ROOT_LOGGER.trace("InboundBridgeService.stop");

        removeOrphanFilter();
        removeRecoveryModule();
    }

    @Override
    public InboundBridgeService getValue() throws IllegalStateException, IllegalArgumentException {
        RTSLogger.ROOT_LOGGER.trace("InboundBridgeService.getValue");

        return this;
    }

    private void addDeserializerAndOrphanFilter() {
        final RecoveryManager recoveryManager = RecoveryManager.manager();

        for (RecoveryModule recoveryModule : recoveryManager.getModules()) {
            if (recoveryModule instanceof XARecoveryModule) {
                orphanFilter = new InboundBridgeOrphanFilter();
                ((XARecoveryModule) recoveryModule).addXAResourceOrphanFilter(orphanFilter);
                ((XARecoveryModule) recoveryModule).addSerializableXAResourceDeserializer(new InboundBridge());
            }
        }
    }

    private void removeOrphanFilter() {
        if (orphanFilter == null) {
            return;
        }

        final RecoveryManager recoveryManager = RecoveryManager.manager();

        for (RecoveryModule recoveryModule : recoveryManager.getModules()) {
            if (recoveryModule instanceof XARecoveryModule) {
                ((XARecoveryModule) recoveryModule).removeXAResourceOrphanFilter(orphanFilter);
            }
        }
    }

    private void addRecoveryModule() {
        final RecoveryManager recoveryManager = RecoveryManager.manager();

        recoveryModule = new InboundBridgeRecoveryModule();
        recoveryManager.addModule(recoveryModule);
    }

    private void removeRecoveryModule() {
        if (recoveryModule == null) {
            return;
        }

        final RecoveryManager recoveryManager = RecoveryManager.manager();

        recoveryManager.removeModule(recoveryModule, false);
    }
}
