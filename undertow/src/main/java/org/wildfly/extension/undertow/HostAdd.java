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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.msc.service.ServiceBuilder.DependencyType.OPTIONAL;
import static org.jboss.msc.service.ServiceBuilder.DependencyType.REQUIRED;

import java.util.LinkedList;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ServiceVerificationHandler;
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

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class HostAdd extends AbstractAddStepHandler {

    static final HostAdd INSTANCE = new HostAdd();

    private HostAdd() {
        super(HostDefinition.ALIAS, HostDefinition.DEFAULT_WEB_MODULE);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final PathAddress parent = address.subAddress(0, address.size() - 1);
        final String name = address.getLastElement().getValue();
        List<String> aliases = HostDefinition.ALIAS.unwrap(context, model);
        String defaultWebModule = HostDefinition.DEFAULT_WEB_MODULE.resolveModelAttribute(context, model).asString();
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        Resource accessLog = resource.getChild(UndertowExtension.PATH_ACCESS_LOG);

        final String serverName = parent.getLastElement().getValue();
        final ServiceName virtualHostServiceName = UndertowService.virtualHostName(serverName, name);
        final ServiceName accessLogServiceName = UndertowService.accessLogServiceName(serverName, name);
        Host service = new Host(name, aliases == null ? new LinkedList<String>() : aliases, defaultWebModule);
        final ServiceBuilder<Host> builder = context.getServiceTarget().addService(virtualHostServiceName, service)
                .addDependency(UndertowService.SERVER.append(serverName), Server.class, service.getServerInjection())
                .addDependency(UndertowService.UNDERTOW, UndertowService.class, service.getUndertowService())
                .addDependency(accessLog != null ? REQUIRED : OPTIONAL, accessLogServiceName, AccessLogService.class, service.getAccessLogService());

        builder.addListener(verificationHandler);
        builder.setInitialMode(Mode.ON_DEMAND);


        final ServiceController<WebHost> commonController = addCommonHost(context, verificationHandler, name, aliases, serverName, virtualHostServiceName);

        final ServiceController<Host> serviceController = builder.install();

        // Setup the web console redirect
        final ServiceName consoleRedirectName = UndertowService.consoleRedirectServiceName(serverName, name);
        final ServiceController<ConsoleRedirectService> consoleServiceServiceController;
        // A standalone server is the only process type with a console redirect
        if (context.getProcessType() == ProcessType.STANDALONE_SERVER) {
            final ConsoleRedirectService redirectService = new ConsoleRedirectService();
            final ServiceBuilder<ConsoleRedirectService> redirectBuilder = context.getServiceTarget().addService(consoleRedirectName, redirectService)
                    .addDependency(UndertowHttpManagementService.SERVICE_NAME, HttpManagement.class, redirectService.getHttpManagementInjector())
                    .addDependency(virtualHostServiceName, Host.class, redirectService.getHostInjector())
                    .setInitialMode(Mode.PASSIVE);
            consoleServiceServiceController = redirectBuilder.install();
        } else {
            // Other process types don't have a console, not depending on the UndertowHttpManagementService should
            // result in a null dependency in the service and redirect accordingly
            final ConsoleRedirectService redirectService = new ConsoleRedirectService();
            final ServiceBuilder<ConsoleRedirectService> redirectBuilder = context.getServiceTarget().addService(consoleRedirectName, redirectService)
                    .addDependency(virtualHostServiceName, Host.class, redirectService.getHostInjector())
                    .setInitialMode(Mode.PASSIVE);
            consoleServiceServiceController = redirectBuilder.install();
        }

        if (newControllers != null) {
            newControllers.add(serviceController);
            newControllers.add(consoleServiceServiceController);
            newControllers.add(commonController);
        }
    }

    private ServiceController<WebHost> addCommonHost(OperationContext context, ServiceVerificationHandler verificationHandler, String hostName, List<String> aliases,
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

        builder.addListener(verificationHandler);
        builder.setInitialMode(Mode.PASSIVE);
        return builder.install();
    }
}
