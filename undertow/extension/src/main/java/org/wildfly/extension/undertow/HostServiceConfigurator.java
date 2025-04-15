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
import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.server.mgmt.UndertowHttpManagementService;
import org.jboss.as.server.mgmt.domain.HttpManagement;
import org.jboss.as.server.suspend.SuspendController;
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
import org.wildfly.undertow.service.HostServiceInstallerProvider;

/**
 * Configures runtime resources for a host resource
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author Paul Ferraro
 */
enum HostServiceConfigurator implements ResourceServiceConfigurator {
    INSTANCE;

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        final PathAddress address = context.getCurrentAddress();
        final PathAddress serverAddress = address.getParent();
        final PathAddress subsystemAddress = serverAddress.getParent();
        final ModelNode subsystemModel = Resource.Tools.readModel(context.readResourceFromRoot(subsystemAddress, false), 0);
        final ModelNode serverModel = Resource.Tools.readModel(context.readResourceFromRoot(serverAddress, false), 0);

        final String name = address.getLastElement().getValue();
        final List<String> aliases = HostDefinition.ALIAS.unwrap(context, model);
        final String defaultWebModule = HostDefinition.DEFAULT_WEB_MODULE.resolveModelAttribute(context, model).asString();
        final String defaultServerName = UndertowRootDefinition.DEFAULT_SERVER.resolveModelAttribute(context, subsystemModel).asString();
        final String defaultHostName = ServerDefinition.DEFAULT_HOST.resolveModelAttribute(context, serverModel).asString();
        final String serverName = serverAddress.getLastElement().getValue();
        final boolean isDefaultHost = defaultServerName.equals(serverName) && name.equals(defaultHostName);
        final int defaultResponseCode = HostDefinition.DEFAULT_RESPONSE_CODE.resolveModelAttribute(context, model).asInt();
        final boolean enableConsoleRedirect = !HostDefinition.DISABLE_CONSOLE_REDIRECT.resolveModelAttribute(context, model).asBoolean();
        Boolean queueRequestsOnStart = HostDefinition.QUEUE_REQUESTS_ON_START.resolveModelAttribute(context, model).asBooleanOrNull();

        // TODO Replace DefaultDeploymentMappingProvider with default-server, default-host capabilities
        if (!defaultWebModule.equals(HostDefinition.DEFAULT_WEB_MODULE_DEFAULT) || DefaultDeploymentMappingProvider.instance().getMapping(HostDefinition.DEFAULT_WEB_MODULE_DEFAULT) == null) {
            DefaultDeploymentMappingProvider.instance().addMapping(defaultWebModule, serverName, name);
        }

        List<ResourceServiceInstaller> installers = new LinkedList<>();
        installers.add(new ResourceServiceInstaller() {
            @Override
            public Consumer<OperationContext> install(OperationContext context) {
                final CapabilityServiceBuilder<?> builder = context.getCapabilityServiceTarget().addCapability(HostDefinition.HOST_CAPABILITY);
                // TODO Default host logic belongs in ServerAdd
                Consumer<Host> hostConsumer = isDefaultHost ? builder.provides(HostDefinition.HOST_CAPABILITY, UndertowService.DEFAULT_HOST) : builder.provides(HostDefinition.HOST_CAPABILITY);
                Supplier<Server> server = builder.requires(Server.SERVICE_DESCRIPTOR, serverName);
                Supplier<UndertowService> service = builder.requiresCapability(Capabilities.CAPABILITY_UNDERTOW, UndertowService.class);
                Supplier<ControlledProcessStateService> processState = builder.requires(ControlledProcessStateService.SERVICE_NAME);
                Supplier<SuspendController> scSupplier = builder.requires(context.getCapabilityServiceName(Capabilities.REF_SUSPEND_CONTROLLER, SuspendController.class));
                ServiceController<?> controller = builder.setInstance(new Host(hostConsumer, server, service, processState, scSupplier, name, aliases, defaultWebModule, defaultResponseCode, queueRequestsOnStart))
                        .setInitialMode(Mode.ON_DEMAND)
                        .install();
                return new Consumer<>() {
                    @Override
                    public void accept(OperationContext context) {
                        DefaultDeploymentMappingProvider.instance().removeMapping(defaultWebModule);
                        context.removeService(controller);
                    }
                };
            }
        });
        // TODO This belongs in ServerAdd, which defines the default host
        if (isDefaultHost) {
            installers.add(new CapabilityServiceInstaller() {
                @Override
                public ServiceController<?> install(CapabilityServiceTarget target) {
                    final RuntimeCapability<?>[] capabilitiesParam = new RuntimeCapability<?>[] {WebHost.CAPABILITY};
                    final ServiceName[] serviceNamesParam = new ServiceName[aliases.size() + 1];
                    int i = 0;
                    for (final String alias : aliases) {
                        serviceNamesParam[i++] = WebHost.SERVICE_NAME.append(alias);
                    }
                    serviceNamesParam[serviceNamesParam.length - 1] = WebHost.SERVICE_NAME.append(context.getCurrentAddressValue());
                    final boolean rqCapabilityAvailable = context.hasOptionalCapability(Capabilities.REF_REQUEST_CONTROLLER, WebHost.CAPABILITY.getDynamicName(context.getCurrentAddress()), null);

                    final CapabilityServiceBuilder<?> sb = context.getCapabilityServiceTarget().addCapability(WebHost.CAPABILITY);
                    final Consumer<WebHost> whConsumer = sb.provides(capabilitiesParam, serviceNamesParam);
                    final Supplier<Server> sSupplier = sb.requires(Server.SERVICE_DESCRIPTOR, serverName);
                    final Supplier<Host> hSupplier = sb.requires(Host.SERVICE_DESCRIPTOR, serverName, name);
                    final Supplier<RequestController> rcSupplier = rqCapabilityAvailable ? sb.requiresCapability(Capabilities.REF_REQUEST_CONTROLLER, RequestController.class) : null;
                    sb.setInstance(new WebHostService(whConsumer, sSupplier, hSupplier, rcSupplier));
                    sb.requiresCapability(CommonWebServer.CAPABILITY_NAME, CommonWebServer.class);
                    sb.setInitialMode(Mode.PASSIVE);
                    return sb.install();
                }
            });
        }
        if (enableConsoleRedirect) {
            // Setup the web console redirect
            installers.add(new ServiceInstaller() {
                @Override
                public ServiceController<?> install(RequirementServiceTarget target) {
                    RequirementServiceBuilder<?> builder = target.addService();
                    Supplier<Host> host = builder.requires(Host.SERVICE_DESCRIPTOR, serverName, name);
                    Supplier<HttpManagement> management = (context.getProcessType() == ProcessType.STANDALONE_SERVER) ? builder.requires(UndertowHttpManagementService.SERVICE_NAME) : null;
                    return builder.setInstance(new ConsoleRedirectService(management, host))
                            .setInitialMode(ServiceController.Mode.PASSIVE)
                            .install();
                }
            });
        }

        for (HostServiceInstallerProvider provider : ServiceLoader.load(HostServiceInstallerProvider.class, HostServiceInstallerProvider.class.getClassLoader())) {
            installers.add(provider.getServiceInstaller(serverName, name));
        }

        return ResourceServiceInstaller.combine(installers);
    }
}
