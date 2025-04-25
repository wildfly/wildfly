/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.mgmt.UndertowHttpManagementService;
import org.jboss.as.server.mgmt.domain.HttpManagement;
import org.jboss.as.server.suspend.SuspendableActivityRegistry;
import org.jboss.as.web.host.CommonWebServer;
import org.jboss.as.web.host.WebHost;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.requestcontroller.RequestController;
import org.wildfly.extension.undertow.deployment.DefaultDeploymentMappingProvider;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Configures services for a host resource.
 * @author Paul Ferraro
 */
public enum HostServiceConfigurator implements ResourceServiceConfigurator {
    INSTANCE;

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        final PathAddress address = context.getCurrentAddress();
        final PathAddress serverAddress = address.getParent();
        final PathAddress subsystemAddress = serverAddress.getParent();
        // TODO Get rid of these model reads
        final ModelNode subsystemModel = Resource.Tools.readModel(context.readResourceFromRoot(subsystemAddress, false), 0);
        final ModelNode serverModel = Resource.Tools.readModel(context.readResourceFromRoot(serverAddress, false), 0);

        final String name = address.getLastElement().getValue();
        final List<String> aliases = HostDefinition.ALIAS.unwrap(context, model);
        final String defaultWebModule = HostDefinition.DEFAULT_WEB_MODULE.resolveModelAttribute(context, model).asString();
        final String defaultServerName = UndertowRootDefinition.DEFAULT_SERVER.resolveModelAttribute(context, subsystemModel).asString();
        final String defaultHostName = ServerDefinition.DEFAULT_HOST.resolveModelAttribute(context, serverModel).asString();
        final String serverName = serverAddress.getLastElement().getValue();
        // TODO Move default host logic to the server's runtime handler where it belongs
        final boolean isDefaultHost = defaultServerName.equals(serverName) && name.equals(defaultHostName);
        final int defaultResponseCode = HostDefinition.DEFAULT_RESPONSE_CODE.resolveModelAttribute(context, model).asInt();
        final boolean enableConsoleRedirect = !HostDefinition.DISABLE_CONSOLE_REDIRECT.resolveModelAttribute(context, model).asBoolean();
        Boolean queueRequestsOnStart = HostDefinition.QUEUE_REQUESTS_ON_START.resolveModelAttribute(context, model).asBooleanOrNull();

        List<ResourceServiceInstaller> installers = new LinkedList<>();
        installers.add(new CapabilityServiceInstaller() {
            @Override
            public ServiceController<?> install(CapabilityServiceTarget target) {
                final CapabilityServiceBuilder<?> builder = target.addCapability(HostDefinition.HOST_CAPABILITY);
                Consumer<Host> hostConsumer = isDefaultHost ? builder.provides(HostDefinition.HOST_CAPABILITY, UndertowService.DEFAULT_HOST) : builder.provides(HostDefinition.HOST_CAPABILITY);
                final Supplier<Server> server = builder.requires(Server.SERVICE_DESCRIPTOR, serverName);
                final Supplier<ProcessStateNotifier> notifier = builder.requires(ProcessStateNotifier.SERVICE_DESCRIPTOR);
                final Supplier<SuspendableActivityRegistry> suspendController = builder.requires(SuspendableActivityRegistry.SERVICE_DESCRIPTOR);
                builder.setInstance(new Host(hostConsumer, server, notifier, suspendController, name, aliases == null ? new LinkedList<>(): aliases, defaultWebModule, defaultResponseCode, queueRequestsOnStart));
                builder.setInitialMode(Mode.ON_DEMAND);
                return builder.install();
            }

            @Override
            public Consumer<OperationContext> install(OperationContext context) {
                // DefaultDeploymentMappingProvider logic implies a missing capability constraint, i.e. that the default web module be unique per host
                // TODO Replace this will capability reference in DUP
                if (!defaultWebModule.equals(HostDefinition.DEFAULT_WEB_MODULE_DEFAULT) || DefaultDeploymentMappingProvider.instance().getMapping(HostDefinition.DEFAULT_WEB_MODULE_DEFAULT) == null) {
                    DefaultDeploymentMappingProvider.instance().addMapping(defaultWebModule, serverName, name);
                }
                // Also remove mapping
                // Though this does not make sense if we were not the host that added it (e.g. for ROOT.war)
                // Alas, this was the existing logic
                return CapabilityServiceInstaller.super.install(context).andThen(ctx -> DefaultDeploymentMappingProvider.instance().removeMapping(defaultWebModule));
            }
        });

        if (enableConsoleRedirect) {
            // A standalone server is the only process type with a console redirect
            // Other process types don't have a console, not depending on the UndertowHttpManagementService should
            // result in a null dependency in the service and redirect accordingly
            boolean handleConsoleRedirect = context.getProcessType() == ProcessType.STANDALONE_SERVER;
            installers.add(new ServiceInstaller() {
                @Override
                public ServiceController<?> install(RequirementServiceTarget target) {
                    // Setup the web console redirect
                    final RequirementServiceBuilder<?> builder = target.addService();
                    final Supplier<HttpManagement> httpManagement = handleConsoleRedirect ? builder.requires(UndertowHttpManagementService.SERVICE_NAME) : null;
                    final Supplier<Host> host = builder.requires(Host.SERVICE_DESCRIPTOR, serverName, name);
                    builder.setInstance(new ConsoleRedirectService(httpManagement, host));
                    builder.setInitialMode(Mode.PASSIVE);
                    return builder.install();
                }
            });
        }

        if (isDefaultHost) {
            // TODO Move this to the runtime handler of the server resource, which defines the default host
            final RuntimeCapability<?>[] capabilitiesParam = new RuntimeCapability<?>[] { WebHost.CAPABILITY };
            final ServiceName[] serviceNamesParam = new ServiceName[aliases == null ? 1 : aliases.size() + 1];
            if (aliases != null) {
                int i = 0;
                for (final String alias : aliases) {
                    serviceNamesParam[i++] = WebHost.SERVICE_NAME.append(alias);
                }
            }
            serviceNamesParam[serviceNamesParam.length - 1] = WebHost.SERVICE_NAME.append(context.getCurrentAddressValue());
            final boolean rqCapabilityAvailable = context.hasOptionalCapability(Capabilities.REF_REQUEST_CONTROLLER, WebHost.CAPABILITY.getDynamicName(context.getCurrentAddress()), null);
            installers.add(new CapabilityServiceInstaller() {
                @Override
                public ServiceController<?> install(CapabilityServiceTarget target) {
                    final CapabilityServiceBuilder<?> builder = target.addCapability(WebHost.CAPABILITY);
                    final Consumer<WebHost> injector = builder.provides(capabilitiesParam, serviceNamesParam);
                    final Supplier<Server> server = builder.requires(Server.SERVICE_DESCRIPTOR, serverName);
                    final Supplier<Host> host = builder.requires(Host.SERVICE_DESCRIPTOR, serverName, name);
                    final Supplier<RequestController> requestController = rqCapabilityAvailable ? builder.requiresCapability(Capabilities.REF_REQUEST_CONTROLLER, RequestController.class) : null;
                    builder.setInstance(new WebHostService(injector, server, host, requestController));
                    builder.requiresCapability(CommonWebServer.CAPABILITY_NAME, CommonWebServer.class);
                    builder.setInitialMode(Mode.PASSIVE);
                    return builder.install();
                }
            });
        }

        // Install any provided services
        for (HostServiceInstallerProvider provider : ServiceLoader.load(HostServiceInstallerProvider.class, HostServiceInstallerProvider.class.getClassLoader())) {
            installers.add(provider.getServiceInstaller(serverName, name));
        }

        return ResourceServiceInstaller.combine(installers);
    }
}
