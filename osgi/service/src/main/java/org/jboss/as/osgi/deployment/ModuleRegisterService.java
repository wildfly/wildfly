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

package org.jboss.as.osgi.deployment;

import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.osgi.OSGiMessages.MESSAGES;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.jboss.osgi.resolver.XResourceBuilderFactory;

/**
 * Service responsible for creating and managing the life-cycle of an OSGi deployment.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 03-Jun-2011
 */
public class ModuleRegisterService implements Service<ModuleRegisterService> {

    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("osgi", "registration");

    private final Module module;
    private final OSGiMetaData metadata;
    private final InjectedValue<XEnvironment> injectedEnvironment = new InjectedValue<XEnvironment>();
    private XResource resource;


    private ModuleRegisterService(Module module, OSGiMetaData metadata) {
        this.module = module;
        this.metadata = metadata;
    }

    public static void addService(DeploymentPhaseContext phaseContext, Module module, OSGiMetaData metadata) {
        final ModuleRegisterService service = new ModuleRegisterService(module, metadata);
        final ServiceName serviceName = getServiceName(module.getIdentifier());
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        ServiceBuilder<ModuleRegisterService> builder = serviceTarget.addService(serviceName, service);
        builder.addDependency(Services.ENVIRONMENT, XEnvironment.class, service.injectedEnvironment);
        builder.addDependency(ServiceModuleLoader.moduleServiceName(module.getIdentifier()));
        builder.install();
    }

    public static void removeService(DeploymentUnit deploymentUnit) {
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        ServiceName serviceName = getServiceName(module.getIdentifier());
        final ServiceController<?> serviceController = deploymentUnit.getServiceRegistry().getService(serviceName);
        if (serviceController != null) {
            serviceController.setMode(Mode.REMOVE);
        }
    }

    public synchronized void start(StartContext context) throws StartException {
        ServiceController<?> controller = context.getController();
        LOGGER.debugf("Starting: %s in mode %s", controller.getName(), controller.getMode());
        LOGGER.infoRegisterModule(module);
        try {
            XResourceBuilder builder = XResourceBuilderFactory.create();
            if (metadata != null) {
                builder.loadFrom(metadata);
            } else {
                builder.loadFrom(module);
            }
            resource = builder.getResource();
            resource.addAttachment(Module.class, module);
            injectedEnvironment.getValue().installResources(resource);
        } catch (Throwable t) {
            throw new StartException(MESSAGES.failedToRegisterModule(module), t);
        }
    }

    public synchronized void stop(StopContext context) {
        ServiceController<?> controller = context.getController();
        LOGGER.debugf("Stopping: %s in mode %s", controller.getName(), controller.getMode());
        if (resource != null) {
            LOGGER.infoUnregisterModule(module);
            injectedEnvironment.getValue().uninstallResources(resource);
        }
    }

    @Override
    public ModuleRegisterService getValue() throws IllegalStateException {
        return this;
    }

    private static ServiceName getServiceName(ModuleIdentifier moduleIdentifier) {
        return SERVICE_NAME_BASE.append(moduleIdentifier.toString());
    }
}