/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.osgi.service;


import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.osgi.parser.SubsystemState.Activation;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.osgi.framework.Services;

/**
 * A service that activates the framework
 *
 * Manual test procedure
 *
 * #1 boot with activation=lazy - The subsystem should not come up
 * #2 activate through the console - Configured capabilities should get installed/resolved/started
 * #3 restart. deploy a bundle through the console - Bundle should get started after capabilities
 * #4 restart with persistent bundle - Persistent bundle should get started after capabilities
 * #5 boot with activation=eager - Persistent bundle should get started after capabilities\
 * #6 remove persistent bundle
 * #7 restart with activation=eager - Configured capabilities should get installed/resolved/started
 *
 * [TODO] [AS7-5217] Add test coverage for OSGi subsystem bootstrap scenarios
 *
 * @author Thomas.Diesler@jboss.com
 * @since 27-Jul-2012
 */
public final class FrameworkActivator {

    private static AtomicBoolean activated;
    private static FrameworkActivator INSTANCE;
    private final ServiceTarget serviceTarget;

    public static void create(ServiceTarget serviceTarget, boolean enabled) {
        INSTANCE = new FrameworkActivator(serviceTarget);
        activated = new AtomicBoolean(!enabled);
    }

    /**
     * Activates the framework.
     */
    public static boolean activate(ServiceVerificationHandler verificationHandler) {
        return INSTANCE.activateInternal(Activation.LAZY, verificationHandler);
    }

    /**
     * Activates the framework eagerly.
     * All services are started and {@link Services#FRAMEWORK_ACTIVE} is verified
     */
    public static boolean activateEagerly(ServiceVerificationHandler verificationHandler) {
        return INSTANCE.activateInternal(Activation.EAGER, verificationHandler);
    }

    private FrameworkActivator(ServiceTarget serviceTarget) {
        this.serviceTarget = serviceTarget;
    }

    private boolean activateInternal(Activation activation, ServiceVerificationHandler verificationHandler) {
        boolean activate = activated.compareAndSet(false, true);
        if (activate) {

            new BootstrapBundlesIntegration().install(serviceTarget, verificationHandler);
            new PersistentBundlesIntegration().install(serviceTarget, verificationHandler);

            ServiceName serviceName = Services.FRAMEWORK_ACTIVE.getParent().append(activation.toString(), "ACTIVATOR");
            switch (activation) {
                case EAGER:
                    EagerActivatorService.addService(serviceTarget, serviceName, verificationHandler);
                    break;
                case LAZY:
                    LazyActivatorService.addService(serviceTarget, serviceName, verificationHandler);
                    break;
            }
        }
        return activate;
    }

    static class EagerActivatorService extends AbstractService<Void> {

        // The {@link EagerActivatorService} has a dependency on {@link Services#FRAMEWORK_ACTIVE}
        static void addService (ServiceTarget serviceTarget, ServiceName serviceName, ServiceVerificationHandler verificationHandler) {
            ServiceBuilder<Void> eagerbuilder = serviceTarget.addService(serviceName, new EagerActivatorService());
            eagerbuilder.addDependency(Services.FRAMEWORK_ACTIVE);
            eagerbuilder.addListener(verificationHandler);
            eagerbuilder.install();
        }
    }

    static class LazyActivatorService extends AbstractService<Void> {

        // The {@link LazyActivatorService} has no framework dependency.
        // Instead it explicitly activates {@link Services#FRAMEWORK_ACTIVE}
        static void addService (ServiceTarget serviceTarget, ServiceName serviceName, ServiceVerificationHandler verificationHandler) {
            ServiceBuilder<Void> eagerbuilder = serviceTarget.addService(serviceName, new LazyActivatorService());
            eagerbuilder.addListener(verificationHandler);
            eagerbuilder.install();
        }

        @Override
        public void start(StartContext context) throws StartException {
            ServiceContainer serviceContainer = context.getController().getServiceContainer();
            serviceContainer.getRequiredService(Services.FRAMEWORK_ACTIVE).setMode(Mode.ACTIVE);
        }
    }
}
