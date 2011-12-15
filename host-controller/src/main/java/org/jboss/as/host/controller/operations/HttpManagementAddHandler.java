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

import java.security.AccessController;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.host.controller.DomainModelControllerService;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.resources.HttpManagementResourceDefinition;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.server.mgmt.HttpManagementService;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.threads.JBossThreadFactory;

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

        if (!context.isBooting()) {
            installHttpManagementServices(context.getServiceTarget(), hostControllerInfo, environment, verificationHandler);
        }
        // else DomainModelControllerService does the service install
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model, List<ServiceController<?>> controllers) {
        HttpManagementRemoveHandler.clearHostControllerInfo(hostControllerInfo);

        if (!context.isBooting()) {
            HttpManagementRemoveHandler.removeHttpManagementService(context);
        }
    }

    static void populateHostControllerInfo(final LocalHostControllerInfoImpl hostControllerInfo, final OperationContext context, final ModelNode model) throws OperationFailedException {
        hostControllerInfo.setHttpManagementInterface(HttpManagementResourceDefinition.INTERFACE.resolveModelAttribute(context, model).asString());
        final ModelNode portNode = HttpManagementResourceDefinition.HTTP_PORT.resolveModelAttribute(context, model);
        hostControllerInfo.setHttpManagementPort(portNode.isDefined() ? portNode.asInt() : -1);
        final ModelNode securePortNode = HttpManagementResourceDefinition.HTTPS_PORT.resolveModelAttribute(context, model);
        hostControllerInfo.setHttpManagementSecurePort(securePortNode.isDefined() ? securePortNode.asInt() : -1);
        final ModelNode realmNode = HttpManagementResourceDefinition.SECURITY_REALM.resolveModelAttribute(context, model);
        hostControllerInfo.setHttpManagementSecurityRealm(realmNode.isDefined() ? realmNode.asString() : null);
    }

    public static void installHttpManagementServices(final ServiceTarget serviceTarget, final LocalHostControllerInfo hostControllerInfo,
                                               final HostControllerEnvironment environment,
                                               final ServiceVerificationHandler verificationHandler) {

        String interfaceName = hostControllerInfo.getHttpManagementInterface();
        int port = hostControllerInfo.getHttpManagementPort();
        int securePort = hostControllerInfo.getHttpManagementSecurePort();
        String securityRealm = hostControllerInfo.getHttpManagementSecurityRealm();

        StringBuilder sb = new StringBuilder();
        sb.append("creating http management service using network interface (").append(interfaceName).append(")");
        if (port > -1) {
            sb.append(" port (").append(port).append(")");
        }
        if (securePort > -1) {
            sb.append(" securePort (").append(securePort).append(")");
        }
        Logger.getLogger("org.jboss.as").info(sb.toString());

        final ThreadFactory httpMgmtThreads = new JBossThreadFactory(new ThreadGroup("HttpManagementService-threads"),
                Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
        final HttpManagementService service = HttpManagementService.createForHost(hostControllerInfo.isMasterDomainController());
        ServiceBuilder<?> builder = serviceTarget.addService(HttpManagementService.SERVICE_NAME, service)
                .addDependency(
                        NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(interfaceName),
                        NetworkInterfaceBinding.class, service.getInterfaceInjector())
                .addDependency(DomainModelControllerService.SERVICE_NAME, ModelController.class, service.getModelControllerInjector())
                .addInjection(service.getPortInjector(), port)
                .addInjection(service.getSecurePortInjector(), securePort)
                .addInjection(service.getExecutorServiceInjector(), Executors.newCachedThreadPool(httpMgmtThreads));

        if (securityRealm != null) {
            builder.addDependency(SecurityRealmService.BASE_SERVICE_NAME.append(securityRealm), SecurityRealmService.class, service.getSecurityRealmInjector());
        } else {
            Logger.getLogger("org.jboss.as").warn("No security realm defined for http management service, all access will be unrestricted.");
        }
        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }

        builder.setInitialMode(ServiceController.Mode.ACTIVE)
                .install();

    }
}
