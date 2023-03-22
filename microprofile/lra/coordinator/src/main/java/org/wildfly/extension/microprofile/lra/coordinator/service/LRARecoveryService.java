/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.lra.coordinator.service;

import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import io.narayana.lra.coordinator.internal.Implementations;
import io.narayana.lra.coordinator.internal.LRARecoveryModule;
import org.jboss.logging.Logger;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.microprofile.lra.coordinator._private.MicroProfileLRACoordinatorLogger;
import org.wildfly.security.manager.WildFlySecurityManager;

public class LRARecoveryService implements Service {
    private static final Logger log = Logger.getLogger(LRARecoveryService.class);

    private volatile LRARecoveryModule lraRecoveryModule;

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        // installing LRA recovery types
        Implementations.install();

        final ClassLoader loader = LRARecoveryService.class.getClassLoader();
        WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(loader);
        try {
            try {
                lraRecoveryModule = LRARecoveryModule.getInstance();
                // verify if the getInstance has added the module else could be removed meanwhile and adding explicitly
                if(RecoveryManager.manager().getModules().stream()
                    .noneMatch(rm -> rm instanceof LRARecoveryModule)) {
                    RecoveryManager.manager().addModule(lraRecoveryModule);
                }
            } catch (Exception e) {
                throw MicroProfileLRACoordinatorLogger.LOGGER.lraRecoveryServiceFailedToStart();
            }
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged((ClassLoader) null);
        }
    }

    @Override
    public synchronized void stop(final StopContext context) {
        Implementations.uninstall();
        if (lraRecoveryModule != null) {
            final RecoveryManager recoveryManager = RecoveryManager.manager();
            try {
                recoveryManager.removeModule(lraRecoveryModule, false);
            } catch (Exception e) {
                log.debugf("Cannot remove LRA recovery module %s while stopping %s",
                    lraRecoveryModule.getClass().getName(), LRARecoveryService.class.getName());
            }
        }

    }
}