/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.server.moduleservice;

import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.Services;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.io.File;

/**
 * Service that manages external modules.
 * <p>
 * Once external modules are installed there is currently no way to safely remove the module spec service, however as they are
 * on-demand services if all dependent services are stopped then the actual {@link Module} will be unloaded.
 * <p>
 * TODO: support removing modules when msc can tell us that nothing depends on the service.
 *
 * @author Stuart Douglas
 *
 */
public class ExternalModuleService implements Service<ExternalModuleService> {

    public static String EXTERNAL_MODULE_PREFIX = ServiceModuleLoader.MODULE_PREFIX + "external.";

    private volatile ServiceContainer serviceContainer;

    public ModuleIdentifier addExternalModule(String externalModule) {
        ModuleIdentifier identifier = ModuleIdentifier.create(EXTERNAL_MODULE_PREFIX + externalModule);
        ServiceName serviceName = ServiceModuleLoader.moduleSpecServiceName(identifier);
        ServiceController<?> controller = serviceContainer.getService(serviceName);
        if (controller == null) {
            ExternalModuleSpecService service = new ExternalModuleSpecService(identifier, new File(externalModule));
            serviceContainer.addService(serviceName, service).setInitialMode(Mode.ON_DEMAND).install();
        }
        return identifier;
    }

    @Override
    public void start(StartContext context) throws StartException {
        if (serviceContainer != null) {
            throw ServerMessages.MESSAGES.externalModuleServiceAlreadyStarted();
        }
        serviceContainer = context.getController().getServiceContainer();
    }

    @Override
    public void stop(StopContext context) {
        serviceContainer = null;
    }

    @Override
    public ExternalModuleService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public static void addService(final ServiceTarget serviceTarget) {
        Service<ExternalModuleService> service = new ExternalModuleService();
        ServiceBuilder<?> serviceBuilder = serviceTarget.addService(Services.JBOSS_EXTERNAL_MODULE_SERVICE, service);
        serviceBuilder.install();
    }
}
