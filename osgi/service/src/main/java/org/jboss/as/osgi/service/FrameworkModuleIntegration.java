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

import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.osgi.parser.SubsystemState.PROP_JBOSS_OSGI_SYSTEM_MODULES;
import static org.jboss.as.osgi.parser.SubsystemState.PROP_JBOSS_OSGI_SYSTEM_MODULES_EXTRA;
import static org.jboss.osgi.framework.Constants.JBOSGI_PREFIX;

import java.util.Map;

import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.osgi.framework.spi.FrameworkModulePlugin;
import org.jboss.osgi.framework.spi.IntegrationService;
import org.osgi.framework.Bundle;

/**
 * An {@link IntegrationService} that provides the Framework module.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
final class FrameworkModuleIntegration implements FrameworkModulePlugin, IntegrationService<FrameworkModulePlugin> {

    private final Map<String, Object> props;
    private Module frameworkModule;

    FrameworkModuleIntegration(Map<String, Object> props) {
        this.props = props;
    }

    @Override
    public ServiceName getServiceName() {
        return FRAMEWORK_MODULE_PLUGIN;
    }

    @Override
    public ServiceController<FrameworkModulePlugin> install(ServiceTarget serviceTarget) {
        ServiceBuilder<FrameworkModulePlugin> builder = serviceTarget.addService(getServiceName(), this);
        builder.setInitialMode(Mode.ON_DEMAND);
        return builder.install();
    }

    @Override
    public void start(StartContext context) throws StartException {
        ServiceController<?> controller = context.getController();
        LOGGER.tracef("Starting: %s in mode %s", controller.getName(), controller.getMode());
    }

    @Override
    public void stop(StopContext context) {
        ServiceController<?> controller = context.getController();
        LOGGER.tracef("Stopping: %s in mode %s", controller.getName(), controller.getMode());
        frameworkModule = null;
    }

    @Override
    public FrameworkModulePlugin getValue() throws IllegalStateException {
        return this;
    }

    @Override
    public Module getFrameworkModule(Bundle systemBundle) {
        if (frameworkModule == null) {
            frameworkModule = createFrameworkModule(systemBundle);
        }
        return frameworkModule;
    }

    private Module createFrameworkModule(final Bundle systemBundle) {
        // Setup the extended framework module spec
        ModuleSpec.Builder specBuilder = ModuleSpec.build(ModuleIdentifier.create(JBOSGI_PREFIX + ".framework"));

        // Add the framework module dependencies
        String sysmodules = (String) props.get(PROP_JBOSS_OSGI_SYSTEM_MODULES);
        if (sysmodules == null)
            sysmodules = "";

        String extramodules = (String) props.get(PROP_JBOSS_OSGI_SYSTEM_MODULES_EXTRA);
        if (extramodules != null)
            sysmodules += "," + extramodules;

        // Add a dependency on the default framework modules
        ModuleLoader bootLoader = Module.getBootModuleLoader();
        PathFilter acceptAll = PathFilters.acceptAll();
        for (String modid : sysmodules.split(",")) {
            modid = modid.trim();
            if (modid.length() > 0) {
                ModuleIdentifier identifier = ModuleIdentifier.create(modid);
                specBuilder.addDependency(DependencySpec.createModuleDependencySpec(acceptAll, acceptAll, bootLoader, identifier, false));
            }
        }

        specBuilder.setModuleClassLoaderFactory(new BundleReferenceClassLoader.Factory(systemBundle));

        try {
            final ModuleSpec moduleSpec = specBuilder.create();
            ModuleLoader moduleLoader = new ModuleLoader() {

                @Override
                protected ModuleSpec findModule(ModuleIdentifier identifier) throws ModuleLoadException {
                    return (moduleSpec.getModuleIdentifier().equals(identifier) ? moduleSpec : null);
                }

                @Override
                public String toString() {
                    return "FrameworkModuleLoader";
                }
            };
            return moduleLoader.loadModule(specBuilder.getIdentifier());
        } catch (ModuleLoadException ex) {
            throw new IllegalStateException(ex);
        }
    }
}