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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.Attachments.BundleState;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XBundleRevision;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.osgi.resolver.XResolveContext;
import org.jboss.osgi.resolver.XResolver;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.ResolutionException;

/**
 * Attach the {@link Module} for a resolved OSGi bundle.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 01-Jul-2012
 */
public class BundleResolveProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        Deployment deployment = depUnit.getAttachment(OSGiConstants.DEPLOYMENT_KEY);
        XBundle bundle = depUnit.getAttachment(OSGiConstants.INSTALLED_BUNDLE_KEY);
        if (bundle == null || deployment.isAutoStart() == false)
            return;

        // Only process the top level deployment
        if (depUnit.getParent() != null)
            return;

        resolveBundle(phaseContext, bundle);
    }

    static void resolveBundle(DeploymentPhaseContext phaseContext, XBundle bundle) {
        XBundleRevision brev = bundle.getBundleRevision();
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        XEnvironment env = depUnit.getAttachment(OSGiConstants.ENVIRONMENT_KEY);
        XResolver resolver = depUnit.getAttachment(OSGiConstants.RESOLVER_KEY);
        XResolveContext context = resolver.createResolveContext(env, Collections.singleton(brev), null);
        try {
            Map<Resource, Wiring> wiremap = resolver.resolveAndApply(context);
            depUnit.putAttachment(Attachments.BUNDLE_STATE_KEY, BundleState.RESOLVED);
            BundleWiring wiring = (BundleWiring) wiremap.get(brev);

            ModuleIdentifier identifier = brev.getModuleIdentifier();
            ServiceName moduleService = ServiceModuleLoader.moduleServiceName(identifier);
            if (phaseContext.getServiceRegistry().getService(moduleService) == null) {
                Set<ServiceName> dependencies = new HashSet<ServiceName>();
                if (wiring != null) {
                    for (BundleWire wire : wiring.getRequiredWires(null)) {
                        XBundleRevision provider = (XBundleRevision) wire.getProvider();
                        ModuleIdentifier providerId = provider.getModuleIdentifier();
                        if (providerId.getName().startsWith(ServiceModuleLoader.MODULE_PREFIX)) {
                            ServiceName moduleSpecService = ServiceModuleLoader.moduleSpecServiceName(identifier);
                            dependencies.add(moduleSpecService);
                        }
                    }
                }
                moduleService = BundleModuleLoadService.addService(phaseContext.getServiceTarget(), identifier, dependencies);
            }
            phaseContext.addDeploymentDependency(moduleService, Attachments.MODULE);
        } catch (ResolutionException ex) {
            LOGGER.warnCannotResolve(ex.getUnresolvedRequirements());
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }

    private static class BundleModuleLoadService implements Service<Module> {

        private final InjectedValue<ModuleLoader> injectedModuleLoader = new InjectedValue<ModuleLoader>();
        private final InjectedValue<ModuleSpec> injectedModuleSpec = new InjectedValue<ModuleSpec>();
        private Module module;

        static ServiceName addService(ServiceTarget target, ModuleIdentifier identifier, Set<ServiceName> moduleSpecDependencies) {
            BundleModuleLoadService service = new BundleModuleLoadService();
            ServiceName serviceName = ServiceModuleLoader.moduleServiceName(identifier);
            ServiceBuilder<Module> builder = target.addService(serviceName, service);
            builder.addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, service.injectedModuleLoader);
            builder.addDependency(ServiceModuleLoader.moduleSpecServiceName(identifier), ModuleSpec.class, service.injectedModuleSpec);
            builder.addDependencies(moduleSpecDependencies);
            builder.setInitialMode(Mode.ON_DEMAND);
            builder.install();
            return serviceName;
        }

        @Override
        public void start(StartContext context) throws StartException {
            ModuleLoader moduleLoader = injectedModuleLoader.getValue();
            ModuleIdentifier identifier = injectedModuleSpec.getValue().getModuleIdentifier();
            try {
                module = moduleLoader.loadModule(identifier);
            } catch (ModuleLoadException e) {
                throw ServerMessages.MESSAGES.failedToLoadModule(identifier, e);
            }
        }

        @Override
        public void stop(StopContext context) {
            // do nothing
        }

        @Override
        public Module getValue() throws IllegalStateException, IllegalArgumentException {
            return module;
        }
    }
}
