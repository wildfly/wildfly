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

import java.util.List;
import java.util.Locale;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;

import org.jboss.as.server.mgmt.HttpManagementService;
import org.jboss.as.server.mgmt.domain.HttpManagement;
import org.jboss.as.server.services.path.AbstractPathService;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * {@code OperationHandler} responsible for adding a virtual host.
 *
 * @author Emanuel Muckenhuber
 * @author Scott stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 */
class WebVirtualHostAdd extends AbstractAddStepHandler implements DescriptionProvider {

    static final WebVirtualHostAdd INSTANCE = new WebVirtualHostAdd();
    private static final String DEFAULT_RELATIVE_TO = "jboss.server.log.dir";
    private static final String TEMP_DIR = "jboss.server.temp.dir";
    private static final String HOME_DIR = "jboss.home.dir";
    private static final String[] NO_ALIASES = new String[0];

    private WebVirtualHostAdd() {
        //
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.get(Constants.ALIAS).set(operation.get(Constants.ALIAS));
        model.get(Constants.ACCESS_LOG).set(operation.get(Constants.ACCESS_LOG));
        model.get(Constants.REWRITE).set(operation.get(Constants.REWRITE));
        model.get(Constants.DEFAULT_WEB_MODULE).set(operation.get(Constants.DEFAULT_WEB_MODULE));

        final boolean welcome = operation.hasDefined(Constants.ENABLE_WELCOME_ROOT) && operation.get(Constants.ENABLE_WELCOME_ROOT).asBoolean();
        model.get(Constants.ENABLE_WELCOME_ROOT).set(welcome);
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();
        boolean welcome = operation.hasDefined(Constants.ENABLE_WELCOME_ROOT) && operation.get(Constants.ENABLE_WELCOME_ROOT).asBoolean();
        final ServiceTarget serviceTarget = context.getServiceTarget();
        final WebVirtualHostService service = new WebVirtualHostService(name, aliases(operation), welcome);
        final ServiceBuilder<?> serviceBuilder = serviceTarget.addService(WebSubsystemServices.JBOSS_WEB_HOST.append(name), service)
                .addDependency(AbstractPathService.pathNameOf(TEMP_DIR), String.class, service.getTempPathInjector())
                .addDependency(WebSubsystemServices.JBOSS_WEB, WebServer.class, service.getWebServer());
        if (operation.hasDefined(Constants.ACCESS_LOG)) {
            final ModelNode accessLog = operation.get(Constants.ACCESS_LOG);
            service.setAccessLog(accessLog.clone());
            // Create the access log service
            accessLogService(name, accessLog, serviceTarget);
            serviceBuilder.addDependency(WebSubsystemServices.JBOSS_WEB_HOST.append(name, Constants.ACCESS_LOG), String.class, service.getAccessLogPathInjector());
        }
        if (operation.hasDefined(Constants.REWRITE)) {
            service.setRewrite(operation.get(Constants.REWRITE).clone());
        }

        if (operation.hasDefined(Constants.DEFAULT_WEB_MODULE)) {
            if (welcome)
                throw new OperationFailedException(new ModelNode().set("A default module can not be specified when the welcome root is enabled."));
            service.setDefaultWebModule(operation.get(Constants.DEFAULT_WEB_MODULE).asString());
        }

        serviceBuilder.addListener(verificationHandler);
        newControllers.add(serviceBuilder.install());

        if (welcome) {
            final WelcomeContextService welcomeService = new WelcomeContextService();
            newControllers.add(context.getServiceTarget().addService(WebSubsystemServices.JBOSS_WEB.append(name).append("welcome"), welcomeService)
                    .addDependency(AbstractPathService.pathNameOf(HOME_DIR), String.class, welcomeService.getPathInjector())
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
            for (int i = 0; i < size; i++) array[i] = aliases.get(i).asString();
            return array;
        }
        return NO_ALIASES;
    }

    static void accessLogService(final String hostName, final ModelNode element, final ServiceTarget target) {
        if (element.has(Constants.ACCESS_LOG)) {
            final ModelNode accessLog = element.get(Constants.ACCESS_LOG);
            final String relativeTo = accessLog.has(RELATIVE_TO) ? accessLog.get(RELATIVE_TO).asString() : DEFAULT_RELATIVE_TO;
            final String path = accessLog.has(PATH) ? accessLog.get(PATH).asString() : hostName;
            RelativePathService.addService(WebSubsystemServices.JBOSS_WEB_HOST.append(hostName, Constants.ACCESS_LOG),
                    path, relativeTo, target);
        } else {
            RelativePathService.addService(WebSubsystemServices.JBOSS_WEB_HOST.append(hostName, Constants.ACCESS_LOG),
                    hostName, DEFAULT_RELATIVE_TO, target);
        }
    }

    static ModelNode getAddOperation(final ModelNode address, final ModelNode subModel) {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);

        if (subModel.hasDefined(Constants.ALIAS)) {
            operation.get(Constants.ALIAS).set(subModel.get(Constants.ALIAS));
        }
        if (subModel.hasDefined(Constants.ACCESS_LOG)) {
            operation.get(Constants.ACCESS_LOG).set(subModel.get(Constants.ACCESS_LOG));
        }
        if (subModel.hasDefined(Constants.REWRITE)) {
            operation.get(Constants.REWRITE).set(subModel.get(Constants.REWRITE));
        }
        if (subModel.hasDefined(Constants.ENABLE_WELCOME_ROOT)) {
            operation.get(Constants.ENABLE_WELCOME_ROOT).set(subModel.get(Constants.ENABLE_WELCOME_ROOT));
        }
        return operation;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return WebSubsystemDescriptions.getVirtualServerAdd(locale);
    }
}
