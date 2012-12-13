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
import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.osgi.OSGiMessages.MESSAGES;
import static org.jboss.as.osgi.parser.SubsystemState.PROP_JBOSS_OSGI_SYSTEM_MODULES;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.osgi.SubsystemExtension;
import org.jboss.as.osgi.management.OSGiRuntimeResource;
import org.jboss.as.osgi.parser.SubsystemState;
import org.jboss.as.osgi.parser.SubsystemState.Activation;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.log.ModuleLogger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceListener.Inheritance;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.spi.FrameworkBuilderFactory;
import org.jboss.osgi.framework.spi.FrameworkBuilder;
import org.jboss.osgi.framework.spi.SystemPaths;
import org.jboss.osgi.framework.spi.FrameworkBuilder.FrameworkPhase;
import org.osgi.framework.Constants;

/**
 * Service responsible for creating and managing the life-cycle of the OSGi Framework.
 *
 * @author Thomas.Diesler@jboss.com
 * @author David Bosschaert
 * @since 11-Sep-2010
 */
public class FrameworkBootstrapService implements Service<Void> {

    static final ServiceName SERVICE_NAME = SERVICE_BASE_NAME.append("framework", "bootstrap");
    static final String MAPPED_OSGI_SOCKET_BINDINGS = "org.jboss.as.osgi.socket.bindings";

    private final InjectedValue<ServerEnvironment> injectedServerEnvironment = new InjectedValue<ServerEnvironment>();
    private final InjectedValue<SubsystemState> injectedSubsystemState = new InjectedValue<SubsystemState>();
    private final ServiceVerificationHandler verificationHandler;
    private final List<SubsystemExtension> extensions;
    private final OSGiRuntimeResource resource;

    public static ServiceController<Void> addService(ServiceTarget target, OSGiRuntimeResource resource, List<SubsystemExtension> extensions, ServiceVerificationHandler verificationHandler) {
        FrameworkBootstrapService service = new FrameworkBootstrapService(resource, extensions, verificationHandler);
        ServiceBuilder<Void> builder = target.addService(FrameworkBootstrapService.SERVICE_NAME, service);
        builder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.injectedServerEnvironment);
        builder.addDependency(OSGiConstants.SUBSYSTEM_STATE_SERVICE_NAME, SubsystemState.class, service.injectedSubsystemState);
        builder.addListener(Inheritance.ONCE, verificationHandler);
        return builder.install();
    }

    private FrameworkBootstrapService(OSGiRuntimeResource resource, List<SubsystemExtension> extensions, ServiceVerificationHandler verificationHandler) {
        this.verificationHandler = verificationHandler;
        this.extensions = extensions;
        this.resource = resource;
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        ServiceController<?> controller = context.getController();
        LOGGER.tracef("Starting: %s in mode %s", controller.getName(), controller.getMode());
        try {
            ServiceContainer serviceContainer = context.getController().getServiceContainer();

            // Setup the OSGi {@link Framework} properties
            SubsystemState subsystemState = injectedSubsystemState.getValue();
            Map<String, Object> props = new HashMap<String, Object>(subsystemState.getProperties());
            setupIntegrationProperties(context, props);

            // Register the URLStreamHandlerFactory
            Module coreFrameworkModule = ((ModuleClassLoader) FrameworkBuilder.class.getClassLoader()).getModule();
            Module.registerURLStreamHandlerFactoryModule(coreFrameworkModule);
            Module.registerContentHandlerFactoryModule(coreFrameworkModule);

            ServiceTarget serviceTarget = context.getChildTarget();
            JAXPServiceProvider.addService(serviceTarget);
            ResolverService.addService(serviceTarget);
            RepositoryService.addService(serviceTarget);

            Activation activation = subsystemState.getActivationPolicy();
            Mode initialMode = (activation == Activation.EAGER ? Mode.ACTIVE : Mode.LAZY);

            // Configure the {@link Framework} builder
            FrameworkBuilder builder = FrameworkBuilderFactory.create(props, initialMode);
            builder.setServiceContainer(serviceContainer);
            builder.setServiceTarget(serviceTarget);

            builder.createFrameworkServices(serviceContainer, true);
            builder.registerIntegrationService(FrameworkPhase.CREATE, new BundleLifecycleIntegration());
            builder.registerIntegrationService(FrameworkPhase.CREATE, new FrameworkModuleIntegration(props));
            builder.registerIntegrationService(FrameworkPhase.CREATE, new ModuleLoaderIntegration());
            builder.registerIntegrationService(FrameworkPhase.CREATE, new SystemServicesIntegration(resource, extensions));
            builder.registerIntegrationService(FrameworkPhase.INIT, new BootstrapBundlesIntegration());
            builder.registerIntegrationService(FrameworkPhase.INIT, new PersistentBundlesIntegration());

            // Add integration services from the extensions
            for(SubsystemExtension extension : extensions) {
                extension.registerIntegrationServices(builder, subsystemState);
            }

            // Install the services to create the framework
            builder.installServices(FrameworkPhase.CREATE, serviceTarget, verificationHandler);

            if (activation == Activation.EAGER) {
                builder.installServices(FrameworkPhase.INIT, serviceTarget, verificationHandler);
                builder.installServices(FrameworkPhase.ACTIVE, serviceTarget, verificationHandler);
            }

            // Create the framework activator
            FrameworkActivator.create(builder);

        } catch (Throwable th) {
            throw MESSAGES.startFailedToCreateFrameworkServices(th);
        }
    }

    @Override
    public synchronized void stop(StopContext context) {
        ServiceController<?> controller = context.getController();
        LOGGER.tracef("Stopping: %s in mode %s", controller.getName(), controller.getMode());
    }

    @Override
    public Void getValue() throws IllegalStateException {
        return null;
    }

    private void setupIntegrationProperties(StartContext context, Map<String, Object> props) {

        // Setup the Framework's storage area.
        String storage = (String) props.get(Constants.FRAMEWORK_STORAGE);
        if (storage == null) {
            ServerEnvironment environment = injectedServerEnvironment.getValue();
            File dataDir = environment.getServerDataDir();
            storage = dataDir.getAbsolutePath() + File.separator + "osgi-store";
            props.put(Constants.FRAMEWORK_STORAGE, storage);
        }

        // Provide the ModuleLogger
        ModuleLogger moduleLogger = Module.getModuleLogger();
        if (moduleLogger != null)
            props.put(ModuleLogger.class.getName(), moduleLogger.getClass().getName());

        // Setup default system modules
        String sysmodules = (String) props.get(PROP_JBOSS_OSGI_SYSTEM_MODULES);
        if (sysmodules == null) {
            Set<String> sysModules = new LinkedHashSet<String>();
            sysModules.addAll(Arrays.asList(SystemPackagesIntegration.DEFAULT_SYSTEM_MODULES));
            sysmodules = sysModules.toString();
            sysmodules = sysmodules.substring(1, sysmodules.length() - 1);
            props.put(PROP_JBOSS_OSGI_SYSTEM_MODULES, sysmodules);
        }

        // Setup default system packages
        String syspackages = (String) getPropertyWithSystemFallback(props, Constants.FRAMEWORK_SYSTEMPACKAGES);
        if (syspackages == null) {
            Set<String> sysPackages = new LinkedHashSet<String>();
            sysPackages.addAll(Arrays.asList(SystemPackagesIntegration.JAVAX_API_PACKAGES));
            sysPackages.addAll(Arrays.asList(SystemPaths.DEFAULT_FRAMEWORK_PACKAGES));
            sysPackages.addAll(Arrays.asList(SystemPackagesIntegration.DEFAULT_INTEGRATION_PACKAGES));
            syspackages = sysPackages.toString();
            syspackages = syspackages.substring(1, syspackages.length() - 1);
            props.put(Constants.FRAMEWORK_SYSTEMPACKAGES, syspackages);
        }

        String extrapackages = (String) getPropertyWithSystemFallback(props, Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
        if (extrapackages != null) {
            props.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, extrapackages);
        }
    }

    // [TODO] Remove this hack when the TCK setup can configure the subsystem properly
    Object getPropertyWithSystemFallback(Map<String, Object> props, String key) {
        Object value = props.get(key);
        if (value == null) {
            value = SecurityActions.getSystemProperty(key);
        }
        return value;
    }
}
