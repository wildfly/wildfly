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

package org.jboss.as.web;

import org.jboss.as.clustering.web.sso.SSOClusterManager;
import org.jboss.as.clustering.web.sso.SSOClusterManagerService;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.AbsolutePathService;
import org.jboss.as.controller.services.path.RelativePathService;
import org.jboss.as.server.mgmt.HttpManagementService;
import org.jboss.as.server.mgmt.domain.HttpManagement;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.web.WebExtension.ACCESS_LOG_PATH;
import static org.jboss.as.web.WebExtension.DIRECTORY_PATH;
import static org.jboss.as.web.WebExtension.SSO_PATH;
import static org.jboss.as.web.WebMessages.MESSAGES;

/**
 * {@code OperationHandler} responsible for adding a virtual host.
 *
 * @author Emanuel Muckenhuber
 * @author Scott stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 * @author Tomaz Cerar
 */
class WebVirtualHostAdd extends AbstractAddStepHandler {

    static final WebVirtualHostAdd INSTANCE = new WebVirtualHostAdd();
    private static final String DEFAULT_RELATIVE_TO = "jboss.server.log.dir";
    private static final String TEMP_DIR = "jboss.server.temp.dir";
    private static final String HOME_DIR = "jboss.home.dir";
    private static final String[] NO_ALIASES = new String[0];

    private WebVirtualHostAdd() {
        //
    }


    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        model.get(WebVirtualHostDefinition.NAME.getName()).set(address.getLastElement().getValue());

        WebVirtualHostDefinition.ALIAS.validateAndSet(operation, model);
        WebVirtualHostDefinition.ENABLE_WELCOME_ROOT.validateAndSet(operation, model);
        WebVirtualHostDefinition.DEFAULT_WEB_MODULE.validateAndSet(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode baseOperation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        ModelNode operation = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        final PathAddress address = PathAddress.pathAddress(baseOperation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        boolean welcome = WebVirtualHostDefinition.ENABLE_WELCOME_ROOT.resolveModelAttribute(context, operation).asBoolean();

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final WebVirtualHostService service = new WebVirtualHostService(name, aliases(operation), welcome);
        final ServiceBuilder<?> serviceBuilder = serviceTarget.addService(WebSubsystemServices.JBOSS_WEB_HOST.append(name), service)
                .addDependency(AbsolutePathService.pathNameOf(TEMP_DIR), String.class, service.getTempPathInjector())
                .addDependency(WebSubsystemServices.JBOSS_WEB, WebServer.class, service.getWebServer());
        if (operation.get(ACCESS_LOG_PATH.getKey(), ACCESS_LOG_PATH.getValue()).isDefined()) {
            final ModelNode accessLog = operation.get(ACCESS_LOG_PATH.getKey(), ACCESS_LOG_PATH.getValue());
            service.setAccessLog(accessLog.clone());
            // Create the access log service
            accessLogService(context,name, accessLog, serviceTarget, newControllers, verificationHandler);
            serviceBuilder.addDependency(WebSubsystemServices.JBOSS_WEB_HOST.append(name, Constants.ACCESS_LOG), String.class, service.getAccessLogPathInjector());
        }
        if (operation.hasDefined(Constants.REWRITE)) {
            service.setRewrite(operation.get(Constants.REWRITE).clone());
        }
        if (operation.get(SSO_PATH.getKey(), SSO_PATH.getValue()).isDefined()) {
            ModelNode sso = operation.get(SSO_PATH.getKey(), SSO_PATH.getValue()).clone();
            service.setSso(sso);
            if (sso.hasDefined(Constants.CACHE_CONTAINER)) {
                ServiceName ssoName = WebSubsystemServices.JBOSS_WEB_HOST.append(name, Constants.SSO);
                serviceBuilder.addDependency(ssoName, SSOClusterManager.class, service.getSSOClusterManager());

                SSOClusterManagerService ssoService = new SSOClusterManagerService();
                SSOClusterManager ssoManager = ssoService.getValue();
                ssoManager.setCacheContainerName(sso.get(Constants.CACHE_CONTAINER).asString());
                if (sso.hasDefined(Constants.CACHE_NAME)) {
                    ssoManager.setCacheName(sso.get(Constants.CACHE_NAME).asString());
                }
                ServiceBuilder<SSOClusterManager> builder = serviceTarget.addService(ssoName, ssoService);
                ssoService.getValue().addDependencies(serviceTarget, builder);
                newControllers.add(builder.setInitialMode(ServiceController.Mode.ON_DEMAND).install());
            }
        }

        if (operation.hasDefined(Constants.DEFAULT_WEB_MODULE)) {
            if (welcome) { throw new OperationFailedException(new ModelNode().set(MESSAGES.noRootWebappWithWelcomeWebapp())); }
            service.setDefaultWebModule(operation.get(Constants.DEFAULT_WEB_MODULE).asString());
        }

        serviceBuilder.addListener(verificationHandler);
        newControllers.add(serviceBuilder.install());

        if (welcome) {
            final WelcomeContextService welcomeService = new WelcomeContextService();
            newControllers.add(context.getServiceTarget().addService(WebSubsystemServices.JBOSS_WEB.append(name).append("welcome"), welcomeService)
                    .addDependency(AbsolutePathService.pathNameOf(HOME_DIR), String.class, welcomeService.getPathInjector())
                    .addDependency(WebSubsystemServices.JBOSS_WEB_HOST.append(name), VirtualHost.class, welcomeService.getHostInjector())
                    .addDependency(ServiceBuilder.DependencyType.OPTIONAL, HttpManagementService.SERVICE_NAME, HttpManagement.class, welcomeService.getHttpManagementInjector())
                    .addListener(verificationHandler)
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install());
        }
    }

    static String[] aliases(final ModelNode node) {
        if (node.hasDefined(Constants.ALIAS)) {
            final ModelNode aliases = node.require(Constants.ALIAS);
            final int size = aliases.asInt();
            final String[] array = new String[size];
            for (int i = 0; i < size; i++) { array[i] = aliases.get(i).asString(); }
            return array;
        }
        return NO_ALIASES;
    }

    static void accessLogService(OperationContext context, final String hostName, final ModelNode node, final ServiceTarget target, List<ServiceController<?>> newControllers, ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        if (node.get(DIRECTORY_PATH.getKey(), DIRECTORY_PATH.getValue()).isDefined()) {
            final ModelNode directory = node.get(DIRECTORY_PATH.getKey(), DIRECTORY_PATH.getValue());

            final String relativeTo = WebAccessLogDirectoryDefinition.RELATIVE_TO.resolveModelAttribute(context, directory).asString();
            final String path = WebAccessLogDirectoryDefinition.PATH.resolveModelAttribute(context, directory).asString();

            RelativePathService.addService(WebSubsystemServices.JBOSS_WEB_HOST.append(hostName, Constants.ACCESS_LOG),
                    path, false, relativeTo, target, newControllers, verificationHandler);
        } else {
            RelativePathService.addService(WebSubsystemServices.JBOSS_WEB_HOST.append(hostName, Constants.ACCESS_LOG),
                    hostName, false, DEFAULT_RELATIVE_TO, target, newControllers, verificationHandler);
        }
    }


}
