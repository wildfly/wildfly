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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ManagementDescription;
import org.jboss.as.controller.remote.ModelControllerClientOperationHandlerFactoryService;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.remoting.RemotingServices;
import org.jboss.as.server.Services;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;


/**
 * The Add handler for the Native Interface when running a standalone server.
 *
 * @author Kabir Khan
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class NativeManagementAddHandler extends AbstractAddStepHandler implements DescriptionProvider {

    public static final NativeManagementAddHandler INSTANCE = new NativeManagementAddHandler();
    public static final String OPERATION_NAME = ModelDescriptionConstants.ADD;

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.get(INTERFACE).set(operation.require(INTERFACE).asString());
        model.get(PORT).set(operation.require(PORT).asInt());
        if (operation.hasDefined(SECURITY_REALM)) {
            model.get(SECURITY_REALM).set(operation.require(SECURITY_REALM).asString());
        }
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final String interfaceName = operation.require(ModelDescriptionConstants.INTERFACE).asString();
        final int port = operation.require(ModelDescriptionConstants.PORT).asInt();

        final ServiceTarget serviceTarget = context.getServiceTarget();

        ServiceName interfaceSvcName = NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(interfaceName);
        ServiceName realmSvcName = null;
        if (operation.hasDefined(SECURITY_REALM)) {
            realmSvcName = SecurityRealmService.BASE_SERVICE_NAME.append(operation.require(SECURITY_REALM).asString());
        }

        RemotingServices.installStandaloneConnectorServices(serviceTarget, interfaceSvcName, port, realmSvcName, verificationHandler, newControllers);
        RemotingServices.installChannelServices(serviceTarget,
                new ModelControllerClientOperationHandlerFactoryService(),
                Services.JBOSS_SERVER_CONTROLLER,
                RemotingServices.MANAGEMENT_CHANNEL,
                verificationHandler,
                newControllers);
    }

    /**
     * {@inheritDoc}
     */
    public ModelNode getModelDescription(Locale locale) {
        return ManagementDescription.getAddNativeManagementDescription(locale);
    }

}
