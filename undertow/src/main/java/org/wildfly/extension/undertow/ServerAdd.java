/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.AbstractAddStepHandler;
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
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class ServerAdd extends AbstractAddStepHandler {

    ServerAdd() {
        super(ServerDefinition.ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        final PathAddress address = context.getCurrentAddress();
        final PathAddress parentAddress = address.getParent();
        final ModelNode subsystemModel = Resource.Tools.readModel(context.readResourceFromRoot(parentAddress));

        final String name = context.getCurrentAddressValue();
        final String defaultHost = ServerDefinition.DEFAULT_HOST.resolveModelAttribute(context, resource.getModel()).asString();
        final String servletContainer = ServerDefinition.SERVLET_CONTAINER.resolveModelAttribute(context, resource.getModel()).asString();
        final String defaultServerName = UndertowRootDefinition.DEFAULT_SERVER.resolveModelAttribute(context,subsystemModel).asString();

        final ServiceName serverName = UndertowService.SERVER.append(name);
        final Server service = new Server(name, defaultHost);
        final ServiceBuilder<Server> builder = context.getServiceTarget().addService(serverName, service)
                .addDependency(UndertowService.SERVLET_CONTAINER.append(servletContainer), ServletContainerService.class, service.getServletContainerInjector())
                .addDependency(UndertowService.UNDERTOW, UndertowService.class, service.getUndertowServiceInjector());

        builder.setInitialMode(ServiceController.Mode.ACTIVE);
        boolean isDefaultServer = name.equals(defaultServerName);

        if (isDefaultServer) { //only install for default server
            builder.addAliases(UndertowService.DEFAULT_SERVER);//register default server service name

            WebServerService commonWebServer = new WebServerService();
            final ServiceBuilder<WebServerService> commonServerBuilder = context.getServiceTarget().addService(CommonWebServer.SERVICE_NAME, commonWebServer)
                    .addDependency(serverName, Server.class, commonWebServer.getServerInjectedValue())
                    .setInitialMode(ServiceController.Mode.PASSIVE);

            addCommonHostListenerDeps(context, commonServerBuilder, UndertowExtension.HTTP_LISTENER_PATH);
            addCommonHostListenerDeps(context, commonServerBuilder, UndertowExtension.AJP_LISTENER_PATH);
            addCommonHostListenerDeps(context, commonServerBuilder, UndertowExtension.HTTPS_LISTENER_PATH);
            commonServerBuilder.install();

        }
        builder.install();
    }


    private void addCommonHostListenerDeps(OperationContext context, ServiceBuilder<WebServerService> builder, final PathElement listenerPath) {
        ModelNode listeners = Resource.Tools.readModel(context.readResource(PathAddress.pathAddress(listenerPath)), 1);
        if (listeners.isDefined()) {
            for (Property p : listeners.asPropertyList()) {
                for (Property listener : p.getValue().asPropertyList()) {
                    builder.addDependency(UndertowService.listenerName(listener.getName()));
                }
            }
        }
    }
}
