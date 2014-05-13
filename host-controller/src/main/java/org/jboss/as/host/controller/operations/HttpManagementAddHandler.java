/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.host.controller.operations;

import static org.jboss.as.host.controller.HostControllerLogger.AS_ROOT_LOGGER;
import io.undertow.server.ListenerRegistry;

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
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.http.server.ConsoleMode;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.host.controller.DomainModelControllerService;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.resources.HttpManagementResourceDefinition;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.remoting.HttpListenerRegistryService;
import org.jboss.as.remoting.RemotingHttpUpgradeService;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.server.mgmt.UndertowHttpManagementService;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.xnio.OptionMap;

/**
 * A handler that activates the HTTP management API.
 *
 * @author Jason T. Greene
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class HttpManagementAddHandler extends AbstractAddStepHandler {

    public static final String OPERATION_NAME = ModelDescriptionConstants.ADD;

    private final LocalHostControllerInfoImpl hostControllerInfo;
    private final HostControllerEnvironment environment;

    public HttpManagementAddHandler(final LocalHostControllerInfoImpl hostControllerInfo, final HostControllerEnvironment environment) {
        this.hostControllerInfo = hostControllerInfo;
        this.environment = environment;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        for (AttributeDefinition attr : HttpManagementResourceDefinition.ATTRIBUTE_DEFINITIONS) {
            attr.validateAndSet(operation, model);
        }
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        populateHostControllerInfo(hostControllerInfo, context, model);
        // DomainModelControllerService requires this service
        final boolean onDemand = context.isBooting();
        boolean httpUpgrade = HttpManagementResourceDefinition.HTTP_UPGRADE_ENABLED.resolveModelAttribute(context, model).asBoolean();
        installHttpManagementServices(context.getRunningMode(), context.getServiceTarget(), hostControllerInfo, environment, verificationHandler, onDemand, httpUpgrade, context.getServiceRegistry(false), newControllers);
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model, List<ServiceController<?>> controllers) {

        super.rollbackRuntime(context, operation, model, controllers);

        HttpManagementRemoveHandler.clearHostControllerInfo(hostControllerInfo);
    }

    static void populateHostControllerInfo(final LocalHostControllerInfoImpl hostControllerInfo, final OperationContext context, final ModelNode model) throws OperationFailedException {
        hostControllerInfo.setHttpManagementInterface(HttpManagementResourceDefinition.INTERFACE.resolveModelAttribute(context, model).asString());
        final ModelNode portNode = HttpManagementResourceDefinition.HTTP_PORT.resolveModelAttribute(context, model);
        hostControllerInfo.setHttpManagementPort(portNode.isDefined() ? portNode.asInt() : -1);
        final ModelNode secureAddress = HttpManagementResourceDefinition.SECURE_INTERFACE.resolveModelAttribute(context, model);
        hostControllerInfo.setHttpManagementSecureInterface(secureAddress.isDefined() ? secureAddress.asString() : null);
        final ModelNode securePortNode = HttpManagementResourceDefinition.HTTPS_PORT.resolveModelAttribute(context, model);
        hostControllerInfo.setHttpManagementSecurePort(securePortNode.isDefined() ? securePortNode.asInt() : -1);
        final ModelNode realmNode = HttpManagementResourceDefinition.SECURITY_REALM.resolveModelAttribute(context, model);
        hostControllerInfo.setHttpManagementSecurityRealm(realmNode.isDefined() ? realmNode.asString() : null);
    }

    public static void installHttpManagementServices(final RunningMode runningMode, final ServiceTarget serviceTarget, final LocalHostControllerInfo hostControllerInfo,
                                               final HostControllerEnvironment environment,
                                               final ServiceVerificationHandler verificationHandler, boolean onDemand, boolean httpUpgrade, final ServiceRegistry serviceRegistry, final List<ServiceController<?>> newControllers) {

        String interfaceName = hostControllerInfo.getHttpManagementInterface();
        int port = hostControllerInfo.getHttpManagementPort();
        String secureInterfaceName = hostControllerInfo.getHttpManagementSecureInterface();
        int securePort = hostControllerInfo.getHttpManagementSecurePort();
        String securityRealm = hostControllerInfo.getHttpManagementSecurityRealm();

        AS_ROOT_LOGGER.creatingHttpManagementService(interfaceName, port, securePort);

        ConsoleMode consoleMode = ConsoleMode.CONSOLE;
        if (runningMode == RunningMode.ADMIN_ONLY) {
            consoleMode = ConsoleMode.ADMIN_ONLY;
        } else if (!hostControllerInfo.isMasterDomainController()) {
            consoleMode = ConsoleMode.SLAVE_HC;
        }

        final UndertowHttpManagementService service = new UndertowHttpManagementService(consoleMode, environment.getProductConfig().getConsoleSlot());
        ServiceBuilder<?> builder = serviceTarget.addService(UndertowHttpManagementService.SERVICE_NAME, service)
                .addDependency(
                        NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(interfaceName),
                        NetworkInterfaceBinding.class, service.getInterfaceInjector())
                .addDependency(
                        NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(secureInterfaceName),
                        NetworkInterfaceBinding.class, service.getSecureInterfaceInjector())
                .addDependency(DomainModelControllerService.SERVICE_NAME, ModelController.class, service.getModelControllerInjector())
                .addDependency(ControlledProcessStateService.SERVICE_NAME, ControlledProcessStateService.class, service.getControlledProcessStateServiceInjector())
                .addDependency(HttpListenerRegistryService.SERVICE_NAME, ListenerRegistry.class, service.getListenerRegistry())
                .addInjection(service.getPortInjector(), port)
                .addInjection(service.getSecurePortInjector(), securePort);

        if (securityRealm != null) {
            SecurityRealm.ServiceUtil.addDependency(builder, service.getSecurityRealmInjector(), securityRealm, false);
        } else {
            AS_ROOT_LOGGER.noSecurityRealmDefined();
        }
        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }

        builder.setInitialMode(onDemand ? ServiceController.Mode.ON_DEMAND : ServiceController.Mode.ACTIVE)
                .install();

        if(httpUpgrade) {
            ServiceName serverCallbackService = ServiceName.JBOSS.append("host", "controller", "server-inventory", "callback");
            ServiceName tmpDirPath = ServiceName.JBOSS.append("server", "path", "jboss.domain.temp.dir");
            ManagementRemotingServices.installSecurityServices(serviceTarget, ManagementRemotingServices.HTTP_CONNECTOR, securityRealm, serverCallbackService, tmpDirPath, verificationHandler, newControllers);

            NativeManagementServices.installRemotingServicesIfNotInstalled(serviceTarget, hostControllerInfo.getLocalHostName(), verificationHandler, null, serviceRegistry,onDemand);
            final String httpConnectorName;
            if (port > -1 || securePort < 0) {
                httpConnectorName = ManagementRemotingServices.HTTP_CONNECTOR;
            } else {
                httpConnectorName = ManagementRemotingServices.HTTPS_CONNECTOR;
            }

            RemotingHttpUpgradeService.installServices(serviceTarget, ManagementRemotingServices.HTTP_CONNECTOR, httpConnectorName, ManagementRemotingServices.MANAGEMENT_ENDPOINT, OptionMap.EMPTY, verificationHandler, newControllers);
        }

    }
}
