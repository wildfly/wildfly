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


import static org.jboss.as.osgi.OSGiConstants.SERVICE_BASE_NAME;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.osgi.parser.SubsystemState.Activation;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceListener.Inheritance;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.StopContext;
import org.jboss.osgi.framework.Services;

/**
 * A service that activates the framework
 *
 * @author Thomas.Diesler@jboss.com
 * @since 27-Jul-2012
 */
public final class FrameworkActivationService extends AbstractService<Void> {

    private static ServiceName FRAMEWORK_ACTIVATION = SERVICE_BASE_NAME.append("framework", "activation");

    private static AtomicBoolean frameworkActivated = new AtomicBoolean(false);
    private static FrameworkActivationService INSTANCE;
    private final ServiceTarget serviceTarget;

    public static void create(ServiceTarget serviceTarget, Activation activation, ServiceVerificationHandler verificationHandler) {
        INSTANCE = new FrameworkActivationService(serviceTarget);
        if (activation == Activation.EAGER) {
            INSTANCE.activateInternal(verificationHandler);
        }
    }

    public static boolean activateOnce(ServiceVerificationHandler verificationHandler) {
        return INSTANCE.activateInternal(verificationHandler);
    }

    private FrameworkActivationService(ServiceTarget serviceTarget) {
        this.serviceTarget = serviceTarget;
    }

    private boolean activateInternal(ServiceVerificationHandler verificationHandler) {
        boolean activate = frameworkActivated.compareAndSet(false, true);
        if (activate) {
            ServiceBuilder<Void> builder = serviceTarget.addService(FRAMEWORK_ACTIVATION, INSTANCE);
            builder.addListener(Inheritance.ALL, verificationHandler);
            builder.install();
        }
        return activate;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ServiceTarget serviceTarget = context.getChildTarget();

        // Install the bootstrap bundle service
        new BootstrapBundlesIntegration(){
            public void stop(StopContext context) {
                frameworkActivated.set(false);
            }
        }.install(serviceTarget);

        // Install the persistent bundle service
        new PersistentBundlesIntegration().install(serviceTarget);

        // Explicitly activate the Framework
        ServiceContainer serviceContainer = context.getController().getServiceContainer();
        serviceContainer.getRequiredService(Services.FRAMEWORK_ACTIVE).setMode(Mode.ACTIVE);
    }

}
