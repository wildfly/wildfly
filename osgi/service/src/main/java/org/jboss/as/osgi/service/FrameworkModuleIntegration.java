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
import org.jboss.msc.service.StartContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.framework.spi.BundleManager;
import org.jboss.osgi.framework.spi.BundleReferenceClassLoader;
import org.jboss.osgi.framework.spi.FrameworkModuleProvider;
import org.jboss.osgi.framework.spi.FrameworkModuleProviderPlugin;
import org.jboss.osgi.framework.spi.IntegrationService;
import org.osgi.framework.Bundle;

/**
 * An {@link IntegrationService} that provides the Framework module.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
final class FrameworkModuleIntegration extends FrameworkModuleProviderPlugin {

    private final InjectedValue<BundleManager> injectedBundleManager = new InjectedValue<BundleManager>();
    private final Map<String, Object> props;

    FrameworkModuleIntegration(Map<String, Object> props) {
        this.props = props;
    }

    @Override
    protected void addServiceDependencies(ServiceBuilder<FrameworkModuleProvider> builder) {
        super.addServiceDependencies(builder);
        builder.addDependency(Services.BUNDLE_MANAGER, BundleManager.class, injectedBundleManager);
    }

    @Override
    protected FrameworkModuleProvider createServiceValue(StartContext startContext) {
        return new FrameworkModuleProviderImpl();
    }

    class FrameworkModuleProviderImpl implements FrameworkModuleProvider {

        private Module frameworkModule;

        @Override
        public Module getFrameworkModule() {
            synchronized (this) {
                if (frameworkModule == null) {
                    frameworkModule = createFrameworkModule();
                }
                return frameworkModule;
            }
        }

        private Module createFrameworkModule() {
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

            Bundle systemBundle = injectedBundleManager.getValue().getSystemBundle();
            specBuilder.setModuleClassLoaderFactory(new BundleReferenceClassLoader.Factory<Bundle>(systemBundle));

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
}