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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.web.WebExtension.ACCESS_LOG_PATH;
import static org.jboss.as.web.WebExtension.DIRECTORY_PATH;
import static org.jboss.as.web.WebExtension.SSO_PATH;
import static org.jboss.as.web.WebMessages.MESSAGES;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.server.mgmt.UndertowHttpManagementService;
import org.jboss.as.server.mgmt.domain.HttpManagement;
import org.jboss.as.web.deployment.common.JBossWebHost;
import org.jboss.as.web.host.WebHost;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ServiceController.Mode;
import org.wildfly.clustering.web.sso.SSOManagerFactory;
import org.wildfly.clustering.web.sso.SSOManagerFactoryBuilder;
import org.wildfly.clustering.web.sso.SSOManagerFactoryBuilderService;
import org.wildfly.clustering.web.sso.SSOManagerConfiguration;

/**
 * {@code OperationHandler} responsible for adding a virtual host.
 *
 * @author Emanuel Muckenhuber
 * @author Scott stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 * @author Tomaz Cerar
 */
class WebVirtualHostAdd extends AbstractAddStepHandler {

    static final WebVirtualHostAdd INSTANCE = new WebVirtualHostAdd();
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
        ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        final PathAddress address = PathAddress.pathAddress(baseOperation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        boolean welcome = WebVirtualHostDefinition.ENABLE_WELCOME_ROOT.resolveModelAttribute(context, fullModel).asBoolean();

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final WebVirtualHostService service = new WebVirtualHostService(name, aliases(fullModel), welcome, TEMP_DIR);
        final ServiceBuilder<?> serviceBuilder = serviceTarget.addService(WebSubsystemServices.JBOSS_WEB_HOST.append(name), service)
                .addDependency(PathManagerService.SERVICE_NAME, PathManager.class, service.getPathManagerInjector())
                .addDependency(WebSubsystemServices.JBOSS_WEB, WebServer.class, service.getWebServer());

        final JBossWebHost commonWebHost = new JBossWebHost();
        ServiceController<WebHost> commonBuilder = serviceTarget.addService(WebHost.SERVICE_NAME.append(name), commonWebHost)
                .addDependency(WebSubsystemServices.JBOSS_WEB_HOST.append(name), VirtualHost.class, commonWebHost.getInjectedHost())
                .install();
        if(newControllers != null) {
            newControllers.add(commonBuilder);
        }

        if (fullModel.get(ACCESS_LOG_PATH.getKey(), ACCESS_LOG_PATH.getValue()).isDefined()) {
            final ModelNode unresolved = fullModel.get(ACCESS_LOG_PATH.getKey(), ACCESS_LOG_PATH.getValue());

            service.setAccessLog(resolveExpressions(context, unresolved, WebAccessLogDefinition.ACCESS_LOG_ATTRIBUTES));

            final ModelNode accessLogDir = unresolved.get(DIRECTORY_PATH.getKey(), DIRECTORY_PATH.getValue());
            String relativeTo = WebAccessLogDirectoryDefinition.RELATIVE_TO.resolveModelAttribute(context, accessLogDir).asString();
            ModelNode pathNode = WebAccessLogDirectoryDefinition.PATH.resolveModelAttribute(context, accessLogDir);
            service.setAccessLogPaths(pathNode.isDefined() ? pathNode.asString() : name, relativeTo);
        }
        if (fullModel.hasDefined(Constants.REWRITE)) {
            ModelNode resolvedRewrite = resolveRewriteExpressions(context, fullModel.get(Constants.REWRITE));
            service.setRewrite(resolvedRewrite);
        }
        if (fullModel.get(SSO_PATH.getKey(), SSO_PATH.getValue()).isDefined()) {
            ModelNode sso = resolveExpressions(context, fullModel.get(SSO_PATH.getKey(), SSO_PATH.getValue()).clone(), WebSSODefinition.SSO_ATTRIBUTES);
            service.setSso(sso);
            if (sso.hasDefined(Constants.CACHE_CONTAINER)) {
                SSOManagerFactoryBuilder ssoManagerFactoryBuilder = new SSOManagerFactoryBuilderService().getValue();
                if (ssoManagerFactoryBuilder != null) {
                    ServiceName ssoName = WebSubsystemServices.JBOSS_WEB_HOST.append(name, Constants.SSO);
                    serviceBuilder.addDependency(ssoName, SSOManagerFactory.class, service.getSSOManagerFactory());
                    SSOManagerConfiguration ssoManagerConfig = new SSOManagerConfiguration();
                    ssoManagerConfig.setContainer(sso.get(Constants.CACHE_CONTAINER).asString());
                    if (sso.hasDefined(Constants.CACHE_NAME)) {
                        ssoManagerConfig.setCache(sso.get(Constants.CACHE_NAME).asString());
                    }
                    newControllers.add(ssoManagerFactoryBuilder.build(serviceTarget, ssoName, ssoManagerConfig).setInitialMode(Mode.ON_DEMAND).install());
                }
            }
        }

        // Don't follow the standard pattern of resolving and then checking isDefined() here
        // because the default value from resolving will look like a conflict with 'welcome'
        if (fullModel.hasDefined(WebVirtualHostDefinition.DEFAULT_WEB_MODULE.getName())) {
            if (welcome) { throw new OperationFailedException(new ModelNode().set(MESSAGES.noRootWebappWithWelcomeWebapp())); }
            service.setDefaultWebModule(WebVirtualHostDefinition.DEFAULT_WEB_MODULE.resolveModelAttribute(context, fullModel).asString());
        }

        serviceBuilder.addListener(verificationHandler);
        newControllers.add(serviceBuilder.install());

        if (welcome) {
            final WelcomeContextService welcomeService = new WelcomeContextService(HOME_DIR);
            newControllers.add(context.getServiceTarget().addService(WebSubsystemServices.JBOSS_WEB.append(name).append("welcome"), welcomeService)
                    .addDependency(PathManagerService.SERVICE_NAME, PathManager.class, welcomeService.getPathManagerInjector())
                    .addDependency(WebSubsystemServices.JBOSS_WEB_HOST.append(name), VirtualHost.class, welcomeService.getHostInjector())
                    .addDependency(ServiceBuilder.DependencyType.OPTIONAL, UndertowHttpManagementService.SERVICE_NAME, HttpManagement.class, welcomeService.getHttpManagementInjector())
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

    private ModelNode resolveRewriteExpressions(OperationContext context, ModelNode unresolvedRewriteChildren) throws OperationFailedException {
        ModelNode result = new ModelNode();
        for (Property prop : unresolvedRewriteChildren.asPropertyList()) {
            ModelNode resolvedParent = resolveExpressions(context, prop.getValue(), WebReWriteDefinition.ATTRIBUTES);
            result.get(prop.getName()).set(resolvedParent);
            if (prop.getValue().hasDefined(Constants.CONDITION)) {
                for (Property conditionProp : prop.getValue().get(Constants.CONDITION).asPropertyList()) {
                    ModelNode resolvedCondition = resolveExpressions(context, conditionProp.getValue(), WebReWriteConditionDefinition.ATTRIBUTES);
                    result.get(prop.getName()).get(Constants.CONDITION, conditionProp.getName()).set(resolvedCondition);
                }
            }
        }
        return result;
    }

    private ModelNode resolveExpressions(OperationContext context, ModelNode fullModel,
                                         AttributeDefinition...attributeDefinitions) throws OperationFailedException {
        ModelNode result = new ModelNode();
        for (AttributeDefinition def : attributeDefinitions) {
            result.get(def.getName()).set(def.resolveModelAttribute(context, fullModel));
        }
        return result;
    }


}
