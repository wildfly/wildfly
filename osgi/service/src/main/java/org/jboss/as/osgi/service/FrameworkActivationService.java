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

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener.Inheritance;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.osgi.framework.Services;

/**
 * A service that activates the framework
 *
 * @author Thomas.Diesler@jboss.com
 * @since 27-Jul-2012
 */
public final class FrameworkActivationService extends AbstractService<Void> {

    public static final ServiceName FRAMEWORK_ACTIVATION_NAME = SERVICE_BASE_NAME.append("framework", "activation");

    public static ServiceController<?> addService(ServiceTarget target, ServiceVerificationHandler verificationHandler) {
        FrameworkActivationService service = new FrameworkActivationService();
        ServiceBuilder<?> builder = target.addService(FRAMEWORK_ACTIVATION_NAME, service);
        builder.addListener(Inheritance.ALL, verificationHandler);
        return builder.install();
    }

    private FrameworkActivationService() {
    }

    @Override
    public void start(StartContext context) throws StartException {
        ServiceTarget serviceTarget = context.getChildTarget();
        BootstrapBundlesIntegration.addService(serviceTarget);
        PersistentBundlesIntegration.addService(serviceTarget);
        ServiceContainer serviceContainer = context.getController().getServiceContainer();
        serviceContainer.getRequiredService(Services.FRAMEWORK_ACTIVE).setMode(Mode.ACTIVE);
    }
}
