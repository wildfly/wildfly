/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.weld.bootstrap.api.Bootstrap;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * This service calls {@link Bootstrap#endInitialization()}, i.e. places the container into a state where it can service
 * requests.
 *
 * Its start is delayed after all EE components are installed which allows Weld to perform efficient cleanup and further
 * optimizations after bootstrap.
 *
 * @author Martin Kouba
 * @author Matej Novotny
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @see WeldStartService
 */
public class WeldStartCompletionService implements Service {

    public static final ServiceName SERVICE_NAME = ServiceNames.WELD_START_COMPLETION_SERVICE_NAME;

    private final Supplier<WeldBootstrapService> bootstrapSupplier;
    private final List<SetupAction> setupActions;
    private final ClassLoader classLoader;

    private final AtomicBoolean runOnce = new AtomicBoolean();

    public WeldStartCompletionService(final Supplier<WeldBootstrapService> bootstrapSupplier,
                                      final List<SetupAction> setupActions,
                                      final ClassLoader classLoader) {
        this.bootstrapSupplier = bootstrapSupplier;
        this.setupActions = setupActions;
        this.classLoader = classLoader;
    }

    @Override
    public void start(final StartContext context) {
        if (!runOnce.compareAndSet(false, true)) {
            // we only execute this once, if there is a restart, WeldStartService initiates re-deploy hence we do nothing here
            return;
        }
        ClassLoader oldTccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            for (SetupAction action : setupActions) {
                action.setup(null);
            }
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            bootstrapSupplier.get().getBootstrap().endInitialization();
        } finally {
            for (SetupAction action : setupActions) {try {
                action.teardown(null);
            } catch (Exception e) {
                WeldLogger.DEPLOYMENT_LOGGER.exceptionClearingThreadState(e);
            }
            }
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTccl);
        }
    }

    @Override
    public void stop(StopContext context) {
        // No-op
    }

}
