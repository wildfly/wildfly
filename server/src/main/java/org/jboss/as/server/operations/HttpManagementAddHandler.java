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

package org.jboss.as.server.operations;

import static org.jboss.as.server.mgmt.HttpManagementResourceDefinition.HTTPS_PORT;
import static org.jboss.as.server.mgmt.HttpManagementResourceDefinition.HTTP_PORT;
import static org.jboss.as.server.mgmt.HttpManagementResourceDefinition.INTERFACE;
import static org.jboss.as.server.mgmt.HttpManagementResourceDefinition.SECURE_SOCKET_BINDING;
import static org.jboss.as.server.mgmt.HttpManagementResourceDefinition.SECURITY_REALM;
import static org.jboss.as.server.mgmt.HttpManagementResourceDefinition.SOCKET_BINDING;
import io.undertow.server.ListenerRegistry;

import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.domain.http.server.ConsoleMode;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.as.network.SocketBindingManagerImpl;
import org.jboss.as.remoting.HttpListenerRegistryService;
import org.jboss.as.remoting.RemotingHttpUpgradeService;
import org.jboss.as.remoting.RemotingServices;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.server.ServerLogger;
import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.Services;
import org.jboss.as.server.mgmt.HttpManagementResourceDefinition;
import org.jboss.as.server.mgmt.UndertowHttpManagementService;
import org.jboss.as.server.mgmt.domain.HttpManagement;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.xnio.OptionMap;

/**
 * A handler that activates the HTTP management API on a Server.
 *
 * @author Jason T. Greene
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class HttpManagementAddHandler extends AbstractAddStepHandler {

    public static final HttpManagementAddHandler INSTANCE = new HttpManagementAddHandler();
    public static final String OPERATION_NAME = ModelDescriptionConstants.ADD;

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        for (AttributeDefinition definition : HttpManagementResourceDefinition.ATTRIBUTE_DEFINITIONS) {
            validateAndSet(definition, operation, model);
        }
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        //TODO Gigantic HACK to disable the runtime part of this for the core model testing.
        //The core model testing currently uses RunningMode.ADMIN_ONLY, but in the real world
        //the http interface needs to be enabled even when that happens.
        //I don't want to wire up all the services unless I can avoid it, so for now the tests set this system property
        if (WildFlySecurityManager.getPropertyPrivileged("jboss.as.test.disable.runtime", null) != null) {
            return false;
        }
        return true;
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                  final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers)
            throws OperationFailedException {

        boolean httpUpgrade = HttpManagementResourceDefinition.HTTP_UPGRADE_ENABLED.resolveModelAttribute(context, model).asBoolean();
        installHttpManagementConnector(context, model, context.getServiceTarget(), verificationHandler, newControllers, httpUpgrade);
    }

    // TODO move this kind of logic into AttributeDefinition itself
    private static void validateAndSet(final AttributeDefinition definition, final ModelNode operation, final ModelNode subModel) throws OperationFailedException {
        final String attributeName = definition.getName();
        final boolean has = operation.has(attributeName);
        if(! has && definition.isRequired(operation)) {
            throw ServerMessages.MESSAGES.attributeIsRequired(attributeName);
        }
        if(has) {
            if(! definition.isAllowed(operation)) {
                throw ServerMessages.MESSAGES.attributeIsInvalid(attributeName);
            }
            definition.validateAndSet(operation, subModel);
        } else {
            // create the undefined node
            subModel.get(definition.getName());
        }
    }

    // TODO move this kind of logic into AttributeDefinition itself
    private static ModelNode validateResolvedModel(final AttributeDefinition definition, final OperationContext context,
                                                   final ModelNode subModel) throws OperationFailedException {
        final String attributeName = definition.getName();
        final boolean has = subModel.has(attributeName);
        if(! has && definition.isRequired(subModel)) {
            throw ServerMessages.MESSAGES.attributeIsRequired(attributeName);
        }
        ModelNode result;
        if(has) {
            if(! definition.isAllowed(subModel)) {
                if (subModel.hasDefined(attributeName)) {
                    throw ServerMessages.MESSAGES.attributeNotAllowedWhenAlternativeIsPresent(attributeName, Arrays.asList(definition.getAlternatives()));
                } else {
                    // create the undefined node
                    result = new ModelNode();
                }
            } else {
                result = definition.resolveModelAttribute(context, subModel);
            }
        } else {
            // create the undefined node
            result = new ModelNode();
        }

        return result;
    }

    static void installHttpManagementConnector(final OperationContext context, final ModelNode model, final ServiceTarget serviceTarget,
                                               final ServiceVerificationHandler verificationHandler,
                                               final List<ServiceController<?>> newControllers, final boolean httpUpgrade) throws OperationFailedException {

        ServiceName socketBindingServiceName = null;
        ServiceName secureSocketBindingServiceName = null;
        ServiceName interfaceSvcName = null;
        int port = -1;
        int securePort = -1;

        final ModelNode interfaceModelNode = validateResolvedModel(INTERFACE, context, model);
        if (interfaceModelNode.isDefined()) {
            // Legacy config
            String interfaceName = interfaceModelNode.asString();
            interfaceSvcName = NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(interfaceName);
            final ModelNode portNode = HTTP_PORT.resolveModelAttribute(context, model);
            port = portNode.isDefined() ? portNode.asInt() : -1;
            final ModelNode securePortNode = HTTPS_PORT.resolveModelAttribute(context, model);
            securePort = securePortNode.isDefined() ? securePortNode.asInt() : -1;

            // Log the config
            if (securePort > -1) {
                if (port > -1) {
                    ServerLogger.ROOT_LOGGER.creatingHttpManagementServiceOnPortAndSecurePort(interfaceName, port, securePort);
                } else {
                    ServerLogger.ROOT_LOGGER.creatingHttpManagementServiceOnSecurePort(interfaceName, securePort);
                }
            } else if (port > -1) {
                ServerLogger.ROOT_LOGGER.creatingHttpManagementServiceOnPort(interfaceName, port);
            }
        } else {
            // Socket-binding reference based config
            final ModelNode socketBindingNode = SOCKET_BINDING.resolveModelAttribute(context, model);
            if (socketBindingNode.isDefined()) {
                final String bindingName = socketBindingNode.asString();
                socketBindingServiceName = SocketBinding.JBOSS_BINDING_NAME.append(bindingName);
            }
            final ModelNode secureSocketBindingNode = SECURE_SOCKET_BINDING.resolveModelAttribute(context, model);
            if (secureSocketBindingNode.isDefined()) {
                final String bindingName = secureSocketBindingNode.asString();
                secureSocketBindingServiceName = SocketBinding.JBOSS_BINDING_NAME.append(bindingName);
            }

            // Log the config
            if (socketBindingServiceName != null) {
                if (secureSocketBindingServiceName != null) {
                    ServerLogger.ROOT_LOGGER.creatingHttpManagementServiceOnSocketAndSecureSocket(socketBindingServiceName.getSimpleName(),
                            secureSocketBindingServiceName.getSimpleName());
                } else {
                    ServerLogger.ROOT_LOGGER.creatingHttpManagementServiceOnSocket(socketBindingServiceName.getSimpleName());
                }
            } else if (secureSocketBindingServiceName != null) {
                ServerLogger.ROOT_LOGGER.creatingHttpManagementServiceOnSecureSocket(secureSocketBindingServiceName.getSimpleName());
            }
        }

        String securityRealm = null;
        final ModelNode realmNode = SECURITY_REALM.resolveModelAttribute(context, model);
        if (realmNode.isDefined()) {
            securityRealm = realmNode.asString();
        } else {
            ServerLogger.ROOT_LOGGER.httpManagementInterfaceIsUnsecured();
        }
        boolean consoleEnabled = model.get(ModelDescriptionConstants.CONSOLE_ENABLED).asBoolean(true);
        ConsoleMode consoleMode;
        if (consoleEnabled){
            consoleMode = context.getRunningMode() == RunningMode.ADMIN_ONLY ? ConsoleMode.ADMIN_ONLY : ConsoleMode.CONSOLE;
        }else{
            consoleMode = ConsoleMode.NO_CONSOLE;
        }

        ServerEnvironment environment = (ServerEnvironment) context.getServiceRegistry(false).getRequiredService(ServerEnvironmentService.SERVICE_NAME).getValue();
        final UndertowHttpManagementService undertowService = new UndertowHttpManagementService(consoleMode, environment.getProductConfig().getConsoleSlot());
        ServiceBuilder<HttpManagement> undertowBuilder = serviceTarget.addService(UndertowHttpManagementService.SERVICE_NAME, undertowService)
                .addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, undertowService.getModelControllerInjector())
                .addDependency(SocketBindingManagerImpl.SOCKET_BINDING_MANAGER, SocketBindingManager.class, undertowService.getSocketBindingManagerInjector())
                .addDependency(ControlledProcessStateService.SERVICE_NAME, ControlledProcessStateService.class, undertowService.getControlledProcessStateServiceInjector())
                .addDependency(HttpListenerRegistryService.SERVICE_NAME, ListenerRegistry.class, undertowService.getListenerRegistry());

        if (interfaceSvcName != null) {
            undertowBuilder.addDependency(interfaceSvcName, NetworkInterfaceBinding.class, undertowService.getInterfaceInjector())
            .addInjection(undertowService.getPortInjector(), port)
            .addInjection(undertowService.getSecurePortInjector(), securePort);
        } else {
            if (socketBindingServiceName != null) {
                undertowBuilder.addDependency(socketBindingServiceName, SocketBinding.class, undertowService.getSocketBindingInjector());
            }
            if (secureSocketBindingServiceName != null) {
                undertowBuilder.addDependency(secureSocketBindingServiceName, SocketBinding.class, undertowService.getSecureSocketBindingInjector());
            }
        }

        if (securityRealm != null) {
            SecurityRealm.ServiceUtil.addDependency(undertowBuilder, undertowService.getSecurityRealmInjector(), securityRealm, false);
        }

        if (verificationHandler != null) {
            undertowBuilder.addListener(verificationHandler);
        }
        ServiceController<?> controller = undertowBuilder.install();
        if (newControllers != null) {
            newControllers.add(controller);
        }


        if(httpUpgrade) {
            final String hostName = WildFlySecurityManager.getPropertyPrivileged(ServerEnvironment.NODE_NAME, null);


            ServiceName tmpDirPath = ServiceName.JBOSS.append("server", "path", "jboss.server.temp.dir");
            RemotingServices.installSecurityServices(serviceTarget, ManagementRemotingServices.HTTP_CONNECTOR, securityRealm, null, tmpDirPath, verificationHandler, newControllers);
            NativeManagementServices.installRemotingServicesIfNotInstalled(serviceTarget, hostName, verificationHandler, null, context.getServiceRegistry(false));
            RemotingHttpUpgradeService.installServices(serviceTarget, ManagementRemotingServices.HTTP_CONNECTOR, ManagementRemotingServices.HTTP_CONNECTOR, ManagementRemotingServices.MANAGEMENT_ENDPOINT, OptionMap.EMPTY, verificationHandler, newControllers);
        }

    }
}
