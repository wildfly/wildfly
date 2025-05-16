/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import static org.wildfly.extension.undertow.ServerDefinition.SERVER_CAPABILITY;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.web.host.CommonWebServer;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ServerAdd extends AbstractAddStepHandler {

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        final ModelNode parentModel = context.readResourceFromRoot(address.getParent(), false).getModel();

        final String name = context.getCurrentAddressValue();
        final String defaultHost = ServerDefinition.DEFAULT_HOST.resolveModelAttribute(context, resource.getModel()).asString();
        final String servletContainer = ServerDefinition.SERVLET_CONTAINER.resolveModelAttribute(context, resource.getModel()).asString();
        final String defaultServerName = UndertowRootDefinition.DEFAULT_SERVER.resolveModelAttribute(context, parentModel).asString();

        boolean isDefaultServer = name.equals(defaultServerName);
        final CapabilityServiceBuilder<?> sb = context.getCapabilityServiceTarget().addCapability(SERVER_CAPABILITY);
        final Consumer<Server> sConsumer = isDefaultServer ? sb.provides(SERVER_CAPABILITY, UndertowService.DEFAULT_SERVER) : sb.provides(SERVER_CAPABILITY);
        final Supplier<ServletContainerService> scsSupplier = sb.requiresCapability(Capabilities.CAPABILITY_SERVLET_CONTAINER, ServletContainerService.class, servletContainer);
        final Supplier<UndertowService> usSupplier = sb.requiresCapability(Capabilities.CAPABILITY_UNDERTOW, UndertowService.class);
        sb.setInstance(new Server(sConsumer, scsSupplier, usSupplier, name, defaultHost));
        sb.install();

        if (isDefaultServer) { //only install for default server
            final CapabilityServiceBuilder<?> csb = context.getCapabilityServiceTarget().addCapability(CommonWebServer.CAPABILITY);
            final Consumer<WebServerService> wssConsumer = csb.provides(CommonWebServer.CAPABILITY, CommonWebServer.SERVICE_NAME);
            final Supplier<Server> sSupplier = csb.requires(Server.SERVICE_DESCRIPTOR, name);
            csb.setInstance(new WebServerService(wssConsumer, sSupplier));
            csb.setInitialMode(ServiceController.Mode.PASSIVE);

            addCommonHostListenerDeps(context, csb, HttpListenerResourceDefinition.PATH_ELEMENT);
            addCommonHostListenerDeps(context, csb, AjpListenerResourceDefinition.PATH_ELEMENT);
            addCommonHostListenerDeps(context, csb, HttpsListenerResourceDefinition.PATH_ELEMENT);
            csb.install();
        }

        for (ServerServiceInstallerProvider factory : ServiceLoader.load(ServerServiceInstallerProvider.class, ServerServiceInstallerProvider.class.getClassLoader())) {
            factory.getServiceInstaller(name).install(context);
        }
    }

    /**
     * <strong>TODO</strong> WFCORE-6176 Update AbstractAddHandler and its Parameters class to support a more fit-to-use
     * API for handling this kind of use case, i.e. to register a per-capability function to override the default
     * registration logic.
     * <p>
     * Replaces the superclass implementation in order to only register {@link CommonWebServer#CAPABILITY} if
     * the resource's name matches the containing subsystems {@link UndertowRootDefinition#DEFAULT_SERVER} value.
     * <p>
     * <strong>IMPORTANT</strong> This implemenation deliberately doesn't call the superclass implementation
     * as we don't want it to always register {@link CommonWebServer#CAPABILITY}. So we directly handle all
     * registration here.
     *
     * @param context – the context. Will not be null
     * @param operation – the operation that is executing Will not be null
     * @param resource – the resource that has been added. Will reflect any updates made by populateModel(OperationContext, ModelNode, Resource). Will not be null
     */
    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {

        context.registerCapability(SERVER_CAPABILITY.fromBaseCapability(context.getCurrentAddress()));

        // We currently don't have any attributes that reference capabilities, but we have this code in case that changes
        // since we are not calling the superclass code.
        ModelNode model = resource.getModel();

        Map<String, AttributeAccess> attributeAccessMap = context.getResourceRegistration().getAttributes(PathAddress.EMPTY_ADDRESS);
        for (Map.Entry<String, AttributeAccess> entry : attributeAccessMap.entrySet()) {
            AttributeAccess attributeAccess = entry.getValue();
            AttributeDefinition ad = attributeAccess.getAttributeDefinition();
            if (model.hasDefined(ad.getName()) || ad.hasCapabilityRequirements()) {
                ad.addCapabilityRequirements(context, resource, model.get(ad.getName()));
            }
        }

        ModelNode parentModel = context.readResourceFromRoot(context.getCurrentAddress().getParent(), false).getModel();
        final String defaultServerName = UndertowRootDefinition.DEFAULT_SERVER.resolveModelAttribute(context, parentModel).asString();
        boolean isDefaultServer = context.getCurrentAddressValue().equals(defaultServerName);
        if (isDefaultServer) {
            context.registerCapability(CommonWebServer.CAPABILITY);
        }
    }

    private void addCommonHostListenerDeps(OperationContext context, ServiceBuilder<?> builder, final PathElement listenerPath) {
        ModelNode listeners = Resource.Tools.readModel(context.readResource(PathAddress.pathAddress(listenerPath)), 1);
        if (listeners.isDefined()) {
            for (Property p : listeners.asPropertyList()) {
                for (Property listener : p.getValue().asPropertyList()) {
                    builder.requires(ListenerResourceDefinition.LISTENER_CAPABILITY.getCapabilityServiceName(listener.getName()));
                }
            }
        }
    }
}
