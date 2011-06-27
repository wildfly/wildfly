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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;

import java.security.AccessController;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.host.controller.DomainModelControllerService;
import org.jboss.as.host.controller.HostControllerEnvironment;
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
 * {@code OperationStepHandler} for changing attributes on the http management interface.
 *
 * @author Emanuel Muckenhuber
 */
public class HttpManagementAttributeHandlers {

    private HttpManagementAttributeHandlers() {
        //
    }

    public static class HttpManagementAttributeHandler extends WriteAttributeHandlers.WriteAttributeOperationHandler {

        private final LocalHostControllerInfoImpl hostControllerInfo;
        private final HostControllerEnvironment environment;

        public HttpManagementAttributeHandler(final LocalHostControllerInfoImpl hostControllerInfo, final HostControllerEnvironment environment) {
            this.hostControllerInfo = hostControllerInfo;
            this.environment = environment;
        }


        @Override
        protected void modelChanged(final OperationContext context, final ModelNode operation, String attributeName, ModelNode newValue, ModelNode currentValue) throws OperationFailedException {
            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            final ModelNode subModel = resource.getModel();
            if(! newValue.equals(currentValue)) {
                context.addStep(new OperationStepHandler(){
                    @Override
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

                        final String interfaceName = subModel.require(INTERFACE).asString();
                        final int port = subModel.require(ModelDescriptionConstants.PORT).asInt();
                        final String realmName = subModel.hasDefined(SECURITY_REALM) ? subModel.require(SECURITY_REALM).asString() : null;

                        hostControllerInfo.setHttpManagementInterface(interfaceName);
                        hostControllerInfo.setHttpManagementPort(port);
                        hostControllerInfo.setHttpManagementSecurityRealm(realmName);

                        context.removeService(HttpManagementService.SERVICE_NAME);
                        final ServiceVerificationHandler handler = new ServiceVerificationHandler();
                        installHttpManagementServices(context.getServiceTarget(), hostControllerInfo, environment, handler);
                        context.addStep(handler, OperationContext.Stage.VERIFY);
                        context.completeStep();
                    }
                }, OperationContext.Stage.RUNTIME);
            }
            context.completeStep();
        }
    }

    private static void installHttpManagementServices(final ServiceTarget serviceTarget,
                                                      final LocalHostControllerInfo hostControllerInfo,
                                                      final HostControllerEnvironment environment,
                                                      final ServiceVerificationHandler verificationHandler) {

        String interfaceName = hostControllerInfo.getHttpManagementInterface();
        int port = hostControllerInfo.getHttpManagementPort();
        int securePort = hostControllerInfo.getHttpManagementSecurePort();
        String securityRealm = hostControllerInfo.getHttpManagementSecurityRealm();

        Logger.getLogger("org.jboss.as").infof("creating http management service using network interface (%s) port (%s) securePort (%s)", interfaceName, port, securePort);

        final ThreadFactory httpMgmtThreads = new JBossThreadFactory(new ThreadGroup("HttpManagementService-threads"),
                Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
        final HttpManagementService service = new HttpManagementService();
        ServiceBuilder builder = serviceTarget.addService(HttpManagementService.SERVICE_NAME, service)
                .addDependency(
                        NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(interfaceName),
                        NetworkInterfaceBinding.class, service.getInterfaceInjector())
                .addDependency(DomainModelControllerService.SERVICE_NAME, ModelController.class, service.getModelControllerInjector())
                .addInjection(service.getTempDirInjector(), environment.getDomainTempDir().getAbsolutePath())
                .addInjection(service.getPortInjector(), port)
                .addInjection(service.getSecurePortInjector(), securePort)
                .addInjection(service.getExecutorServiceInjector(), Executors.newCachedThreadPool(httpMgmtThreads))
                .addListener(verificationHandler);

        if (securityRealm != null) {
            builder.addDependency(SecurityRealmService.BASE_SERVICE_NAME.append(securityRealm), SecurityRealmService.class, service.getSecurityRealmInjector());
        }
        builder.setInitialMode(ServiceController.Mode.ACTIVE)
                .install();

    }

}
