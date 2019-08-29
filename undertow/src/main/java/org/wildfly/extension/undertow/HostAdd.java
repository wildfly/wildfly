/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import static org.wildfly.extension.undertow.HostDefinition.HOST_CAPABILITY;
import static org.wildfly.extension.undertow.ServerDefinition.SERVER_CAPABILITY;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.server.mgmt.UndertowHttpManagementService;
import org.jboss.as.server.mgmt.domain.HttpManagement;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.as.web.host.CommonWebServer;
import org.jboss.as.web.host.WebHost;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.requestcontroller.RequestController;
import org.wildfly.extension.undertow.deployment.DefaultDeploymentMappingProvider;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class HostAdd extends AbstractAddStepHandler {

    static final HostAdd INSTANCE = new HostAdd();

    private HostAdd() {
        super(HostDefinition.ALIAS, HostDefinition.DEFAULT_WEB_MODULE, HostDefinition.DEFAULT_RESPONSE_CODE, HostDefinition.DISABLE_CONSOLE_REDIRECT, HostDefinition.QUEUE_REQUESTS_ON_START);
    }

    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        super.recordCapabilitiesAndRequirements(context, operation, resource);

        String ourCap = HOST_CAPABILITY.getDynamicName(context.getCurrentAddress());
        String serverCap = SERVER_CAPABILITY.getDynamicName(context.getCurrentAddress().getParent());
        context.registerAdditionalCapabilityRequirement(serverCap, ourCap, null);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
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
        Boolean queueRequestsOnStart = null;

        if (model.hasDefined(HostDefinition.QUEUE_REQUESTS_ON_START.getName())) {
            queueRequestsOnStart = HostDefinition.QUEUE_REQUESTS_ON_START.resolveModelAttribute(context, model).asBoolean();
        }

        if (!defaultWebModule.equals(HostDefinition.DEFAULT_WEB_MODULE_DEFAULT) || DefaultDeploymentMappingProvider.instance().getMapping(HostDefinition.DEFAULT_WEB_MODULE_DEFAULT) == null) {
            DefaultDeploymentMappingProvider.instance().addMapping(defaultWebModule, serverName, name);
        }

        final ServiceName virtualHostServiceName = HostDefinition.HOST_CAPABILITY.fromBaseCapability(address).getCapabilityServiceName();

        final CapabilityServiceBuilder<?> csb = context.getCapabilityServiceTarget().addCapability(HostDefinition.HOST_CAPABILITY);
        Consumer<Host> hostConsumer;
        if (isDefaultHost) {
            addCommonHost(context, aliases, serverName, virtualHostServiceName);
            final RuntimeCapability<?>[] capabilitiesParam = new RuntimeCapability<?>[] {HostDefinition.HOST_CAPABILITY};
            final ServiceName[] serviceNamesParam = new ServiceName[] {UndertowService.virtualHostName(serverName, name), UndertowService.DEFAULT_HOST};
            hostConsumer = csb.provides(capabilitiesParam, serviceNamesParam);
        } else {
            hostConsumer = csb.provides(HostDefinition.HOST_CAPABILITY, UndertowService.virtualHostName(serverName, name));
        }
        final Supplier<Server> sSupplier = csb.requiresCapability(Capabilities.CAPABILITY_SERVER, Server.class, serverName);
        final Supplier<UndertowService> usSupplier = csb.requiresCapability(Capabilities.CAPABILITY_UNDERTOW, UndertowService.class);
        final Supplier<ControlledProcessStateService> cpssSupplier = csb.requires(ControlledProcessStateService.SERVICE_NAME);
        final Supplier<SuspendController> scSupplier = csb.requires(context.getCapabilityServiceName(Capabilities.REF_SUSPEND_CONTROLLER, SuspendController.class));
        csb.setInstance(new Host(hostConsumer, sSupplier, usSupplier, cpssSupplier, scSupplier, name, aliases == null ? new LinkedList<>(): aliases, defaultWebModule, defaultResponseCode, queueRequestsOnStart));
        csb.setInitialMode(Mode.ON_DEMAND);
        csb.install();

        if (enableConsoleRedirect) {
            // Setup the web console redirect
            final ServiceName consoleRedirectName = UndertowService.consoleRedirectServiceName(serverName, name);
            // A standalone server is the only process type with a console redirect
            if (context.getProcessType() == ProcessType.STANDALONE_SERVER) {
                final ConsoleRedirectService redirectService = new ConsoleRedirectService();
                final ServiceBuilder<ConsoleRedirectService> redirectBuilder = context.getServiceTarget().addService(consoleRedirectName, redirectService)
                        .addDependency(UndertowHttpManagementService.SERVICE_NAME, HttpManagement.class, redirectService.getHttpManagementInjector())
                        .addDependency(virtualHostServiceName, Host.class, redirectService.getHostInjector())
                        .setInitialMode(Mode.PASSIVE);
                redirectBuilder.install();
            } else {
                // Other process types don't have a console, not depending on the UndertowHttpManagementService should
                // result in a null dependency in the service and redirect accordingly
                final ConsoleRedirectService redirectService = new ConsoleRedirectService();
                final ServiceBuilder<ConsoleRedirectService> redirectBuilder = context.getServiceTarget().addService(consoleRedirectName, redirectService)
                        .addDependency(virtualHostServiceName, Host.class, redirectService.getHostInjector())
                        .setInitialMode(Mode.PASSIVE);
                redirectBuilder.install();
            }
        }
    }

    private void addCommonHost(OperationContext context, List<String> aliases, String serverName, ServiceName virtualHostServiceName) {
        final RuntimeCapability<?>[] capabilitiesParam = new RuntimeCapability<?>[] {WebHost.CAPABILITY};
        final ServiceName[] serviceNamesParam = new ServiceName[aliases == null ? 1 : aliases.size() + 1];
        if (aliases != null) {
            int i = 0;
            for (final String alias : aliases) {
                serviceNamesParam[i++] = WebHost.SERVICE_NAME.append(alias);
            }
        }
        serviceNamesParam[serviceNamesParam.length - 1] = WebHost.SERVICE_NAME.append(context.getCurrentAddressValue());
        final boolean rqCapabilityAvailable = context.hasOptionalCapability(Capabilities.REF_REQUEST_CONTROLLER, null, null);

        final CapabilityServiceBuilder<?> sb = context.getCapabilityServiceTarget().addCapability(WebHost.CAPABILITY);
        final Consumer<WebHost> whConsumer = sb.provides(capabilitiesParam, serviceNamesParam);
        final Supplier<Server> sSupplier = sb.requiresCapability(Capabilities.CAPABILITY_SERVER, Server.class, serverName);
        final Supplier<Host> hSupplier = sb.requires(virtualHostServiceName);
        final Supplier<RequestController> rcSupplier = rqCapabilityAvailable ? sb.requiresCapability(Capabilities.REF_REQUEST_CONTROLLER, RequestController.class) : null;
        sb.setInstance(new WebHostService(whConsumer, sSupplier, hSupplier, rcSupplier));
        sb.requiresCapability(CommonWebServer.CAPABILITY_NAME, CommonWebServer.class);
        sb.setInitialMode(Mode.PASSIVE);
        sb.install();
    }
}
