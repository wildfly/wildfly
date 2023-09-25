/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
