/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import static org.wildfly.extension.undertow.HostDefinition.HOST_CAPABILITY;
import static org.wildfly.extension.undertow.ServerDefinition.SERVER_CAPABILITY;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.Resource;
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
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class HostAdd extends AbstractAddStepHandler {

    static final HostAdd INSTANCE = new HostAdd();

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
        final boolean activeReqTrackingEnabled = HostDefinition.ACTIVE_REQUEST_TRACKING_ENABLED.resolveModelAttribute(context, model).asBoolean();
        final boolean statisticsEnabled = UndertowRootDefinition.STATISTICS_ENABLED.resolveModelAttribute(context, subsystemModel).asBoolean();

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
            hostConsumer = csb.provides(HostDefinition.HOST_CAPABILITY, UndertowService.DEFAULT_HOST);
        } else {
            hostConsumer = csb.provides(HostDefinition.HOST_CAPABILITY);
        }
        final Supplier<Server> sSupplier = csb.requires(Server.SERVICE_DESCRIPTOR, serverName);
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
                final ServiceBuilder<?> sb = context.getServiceTarget().addService(consoleRedirectName);
                final Supplier<HttpManagement> hmSupplier = sb.requires(UndertowHttpManagementService.SERVICE_NAME);
                final Supplier<Host> hSupplier = sb.requires(virtualHostServiceName);
                sb.setInstance(new ConsoleRedirectService(hmSupplier, hSupplier));
                sb.setInitialMode(Mode.PASSIVE);
                sb.install();
            } else {
                // Other process types don't have a console, not depending on the UndertowHttpManagementService should
                // result in a null dependency in the service and redirect accordingly
                final ServiceBuilder<?> sb = context.getServiceTarget().addService(consoleRedirectName);
                final Supplier<Host> hSupplier = sb.requires(virtualHostServiceName);
                sb.setInstance(new ConsoleRedirectService(null, hSupplier));
                sb.setInitialMode(Mode.PASSIVE);
                sb.install();
            }
        }

        if (activeReqTrackingEnabled) {
            if (statisticsEnabled) {
                Predicate predicate = null;
                ModelNode predicateNode = HostDefinition.ACTIVE_REQUEST_TRACKING_PREDICATE.resolveModelAttribute(context, model);
                if(predicateNode.isDefined()) {
                    predicate = Predicates.parse(predicateNode.asString(), getClass().getClassLoader());
                }
                final CapabilityServiceBuilder<?> sb = context.getCapabilityServiceTarget()
                        .addCapability(HostDefinition.ACTIVE_REQUEST_TRACKING_CAPABILITY);
                final Consumer<ActiveRequestTrackerService> serviceConsumer =
                        sb.provides(HostDefinition.ACTIVE_REQUEST_TRACKING_CAPABILITY,
                        UndertowService.activeRequestTrackingServiceName(serverName, name));
                final Supplier<Host> hostSupplier = sb.requiresCapability(Capabilities.CAPABILITY_HOST, Host.class, serverName, name);

                sb.setInstance(new ActiveRequestTrackerService(serviceConsumer, hostSupplier, predicate));
                sb.install();
            } else {
                UndertowLogger.ROOT_LOGGER.cannotEnableActiveRequestTracking();
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
        final boolean rqCapabilityAvailable = context.hasOptionalCapability(Capabilities.REF_REQUEST_CONTROLLER, WebHost.CAPABILITY.getDynamicName(context.getCurrentAddress()), null);

        final CapabilityServiceBuilder<?> sb = context.getCapabilityServiceTarget().addCapability(WebHost.CAPABILITY);
        final Consumer<WebHost> whConsumer = sb.provides(capabilitiesParam, serviceNamesParam);
        final Supplier<Server> sSupplier = sb.requires(Server.SERVICE_DESCRIPTOR, serverName);
        final Supplier<Host> hSupplier = sb.requires(virtualHostServiceName);
        final Supplier<RequestController> rcSupplier = rqCapabilityAvailable ? sb.requiresCapability(Capabilities.REF_REQUEST_CONTROLLER, RequestController.class) : null;
        sb.setInstance(new WebHostService(whConsumer, sSupplier, hSupplier, rcSupplier));
        sb.requiresCapability(CommonWebServer.CAPABILITY_NAME, CommonWebServer.class);
        sb.setInitialMode(Mode.PASSIVE);
        sb.install();
    }
}
