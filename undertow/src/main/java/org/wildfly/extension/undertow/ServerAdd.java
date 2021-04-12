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

import static org.wildfly.extension.undertow.ServerDefinition.SERVER_CAPABILITY;

import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.web.host.CommonWebServer;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.undertow.session.DistributableServerRuntimeHandler;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ServerAdd extends AbstractAddStepHandler {

    ServerAdd() {
        super(new Parameters()
                .addAttribute(ServerDefinition.ATTRIBUTES)
                .addRuntimeCapability(SERVER_CAPABILITY)//only have server capability automatically registered
        );
    }

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
            final Supplier<Server> sSupplier = csb.requiresCapability(Capabilities.CAPABILITY_SERVER, Server.class, name);
            csb.setInstance(new WebServerService(wssConsumer, sSupplier));
            csb.setInitialMode(ServiceController.Mode.PASSIVE);

            addCommonHostListenerDeps(context, csb, UndertowExtension.HTTP_LISTENER_PATH);
            addCommonHostListenerDeps(context, csb, UndertowExtension.AJP_LISTENER_PATH);
            addCommonHostListenerDeps(context, csb, UndertowExtension.HTTPS_LISTENER_PATH);
            csb.install();
        }

        for (DistributableServerRuntimeHandler handler : ServiceLoader.load(DistributableServerRuntimeHandler.class, DistributableServerRuntimeHandler.class.getClassLoader())) {
            handler.execute(context, name);
        }
    }

    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        super.recordCapabilitiesAndRequirements(context, operation, resource);

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
