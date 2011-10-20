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

package org.jboss.as.server.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.remoting.RealmAuthenticationProviderService;
import org.jboss.as.remoting.RealmOptionMapService;
import org.jboss.as.remoting.RemotingServices;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * {@code OperationStepHandler} for changing attributes on the native management interface.
 *
 * @author Emanuel Muckenhuber
 */
public class NativeManagementAttributeHandlers {

    public static OperationStepHandler INSTANCE = new NativeManagementAttributeHandler();

    static class NativeManagementAttributeHandler extends WriteAttributeHandlers.WriteAttributeOperationHandler {

        @Override
        protected void modelChanged(final OperationContext context, final ModelNode operation, String attributeName, ModelNode newValue, ModelNode currentValue) throws OperationFailedException {
            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            final ModelNode subModel = resource.getModel();
            if(! newValue.equals(currentValue)) {
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                        final String interfaceName = subModel.require(ModelDescriptionConstants.INTERFACE).asString();
                        final int port = subModel.require(ModelDescriptionConstants.PORT).asInt();

                        context.removeService(RealmAuthenticationProviderService.createName(ManagementRemotingServices.MANAGEMENT_CONNECTOR));
                        context.removeService(RealmOptionMapService.createName(ManagementRemotingServices.MANAGEMENT_CONNECTOR));
                        context.removeService(RemotingServices.serverServiceName(ManagementRemotingServices.MANAGEMENT_CONNECTOR, port));

                        final ServiceTarget serviceTarget = context.getServiceTarget();
                        final ServiceName interfaceSvcName = NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(interfaceName);
                        ServiceName realmSvcName = null;
                        if (subModel.hasDefined(SECURITY_REALM)) {
                            realmSvcName = SecurityRealmService.BASE_SERVICE_NAME.append(subModel.require(SECURITY_REALM).asString());
                        }
                        List<ServiceController<?>> list =new ArrayList<ServiceController<?>>();
                        final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                        ManagementRemotingServices.installStandaloneConnectorServices(serviceTarget, ManagementRemotingServices.MANAGEMENT_ENDPOINT, interfaceSvcName, port, realmSvcName, verificationHandler, list);

                        context.addStep(verificationHandler, OperationContext.Stage.VERIFY);
                        context.completeStep();
                    }
                }, OperationContext.Stage.RUNTIME);
            }
            context.completeStep();
        }

    }


}
