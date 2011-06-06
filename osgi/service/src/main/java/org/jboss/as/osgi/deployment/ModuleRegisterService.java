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

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.logging.Logger;
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
import org.jboss.osgi.framework.BundleManagerService;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.metadata.OSGiMetaData;

/**
 * Service responsible for creating and managing the life-cycle of an OSGi deployment.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 03-Jun-2011
 */
public class ModuleRegisterService implements Service<ModuleRegisterService> {

    private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("osgi", "registration");

    private final Module module;
    private final OSGiMetaData metadata;
    private InjectedValue<BundleManagerService> injectedBundleManager = new InjectedValue<BundleManagerService>();
    private ServiceName installedBundleName;

    private ModuleRegisterService(Module module, OSGiMetaData metadata) {
        this.module = module;
        this.metadata = metadata;
    }

    public static void addService(DeploymentPhaseContext phaseContext, Module module, OSGiMetaData metadata) {
        final ModuleRegisterService service = new ModuleRegisterService(module, metadata);
        final ServiceName serviceName = getServiceName(module.getIdentifier());
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        ServiceBuilder<ModuleRegisterService> builder = serviceTarget.addService(serviceName, service);
        builder.addDependency(Services.BUNDLE_MANAGER, BundleManagerService.class, service.injectedBundleManager);
        builder.addDependency(ServiceModuleLoader.moduleServiceName(module.getIdentifier()));
        builder.addDependency(Services.FRAMEWORK_ACTIVATOR);
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

    public static ServiceName getServiceName(ModuleIdentifier moduleIdentifier) {
        return ModuleRegisterService.SERVICE_NAME_BASE.append(moduleIdentifier.toString());
    }

    public synchronized void start(StartContext context) throws StartException {
        ServiceController<?> controller = context.getController();
        log.debugf("Starting: %s in mode %s", controller.getName(), controller.getMode());
        log.infof("Register module: %s", module);
        try {
            ServiceTarget serviceTarget = context.getChildTarget();
            BundleManagerService bundleManager = injectedBundleManager.getValue();
            installedBundleName = bundleManager.registerModule(serviceTarget, module, metadata);
        } catch (Throwable t) {
            throw new StartException("Failed to register module: " + module, t);
        }
    }

    public synchronized void stop(StopContext context) {
        ServiceController<?> controller = context.getController();
        log.debugf("Stopping: %s in mode %s", controller.getName(), controller.getMode());
        log.infof("Unregister module: %s", module);
        try {
            BundleManagerService bundleManager = injectedBundleManager.getValue();
            bundleManager.unregisterModule(module.getIdentifier());
        } catch (Throwable t) {
            log.errorf(t, "Failed to uninstall module: %s", module);
        }
    }

    @Override
    public ModuleRegisterService getValue() throws IllegalStateException {
        return this;
    }

    public ServiceName getInstalledBundleName() {
        return installedBundleName;
    }
}
