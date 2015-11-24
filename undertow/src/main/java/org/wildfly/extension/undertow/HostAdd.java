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

import java.util.LinkedList;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.mgmt.UndertowHttpManagementService;
import org.jboss.as.server.mgmt.domain.HttpManagement;
import org.jboss.as.web.host.CommonWebServer;
import org.jboss.as.web.host.WebHost;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.undertow.deployment.DefaultDeploymentMappingProvider;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class HostAdd extends AbstractAddStepHandler {

    static final HostAdd INSTANCE = new HostAdd();

    private HostAdd() {
        super(HostDefinition.ALIAS, HostDefinition.DEFAULT_WEB_MODULE, HostDefinition.DEFAULT_RESPONSE_CODE, HostDefinition.DISABLE_CONSOLE_REDIRECT);
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
        DefaultDeploymentMappingProvider.instance().addMapping(defaultWebModule, serverName, name);

        final ServiceName virtualHostServiceName = UndertowService.virtualHostName(serverName, name);

        final Host service = new Host(name, aliases == null ? new LinkedList<>(): aliases, defaultWebModule, defaultResponseCode);
        final ServiceBuilder<Host> builder = context.getServiceTarget().addService(virtualHostServiceName, service)
                .addDependency(UndertowService.SERVER.append(serverName), Server.class, service.getServerInjection())
                .addDependency(UndertowService.UNDERTOW, UndertowService.class, service.getUndertowService());

        builder.setInitialMode(Mode.ON_DEMAND);

        if (isDefaultHost) {
            addCommonHost(context, name, aliases, serverName, virtualHostServiceName);
            builder.addAliases(UndertowService.DEFAULT_HOST);//add alias for default host of default server service
        }

        builder.install();

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

    private ServiceController<WebHost> addCommonHost(OperationContext context, String hostName, List<String> aliases,
                                                     String serverName, ServiceName virtualHostServiceName) {
        WebHostService service = new WebHostService();
        final ServiceBuilder<WebHost> builder = context.getServiceTarget().addService(WebHost.SERVICE_NAME.append(hostName), service)
                .addDependency(UndertowService.SERVER.append(serverName), Server.class, service.getServer())
                .addDependency(CommonWebServer.SERVICE_NAME)
                .addDependency(virtualHostServiceName, Host.class, service.getHost());

        if (aliases != null) {
            for (String alias : aliases) {
                builder.addAliases(WebHost.SERVICE_NAME.append(alias));
            }
        }

        builder.setInitialMode(Mode.PASSIVE);
        return builder.install();
    }
}
