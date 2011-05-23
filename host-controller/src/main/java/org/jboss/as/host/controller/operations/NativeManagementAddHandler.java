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

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ManagementDescription;
import org.jboss.as.host.controller.mgmt.ManagementCommunicationService;
import org.jboss.as.server.services.net.NetworkInterfaceBinding;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class NativeManagementAddHandler extends AbstractAddStepHandler implements DescriptionProvider {

    public static final NativeManagementAddHandler INSTANCE = new NativeManagementAddHandler();
    public static final String OPERATION_NAME = ModelDescriptionConstants.ADD;

    protected void populateModel(ModelNode operation, ModelNode model) {
        final String interfaceName = operation.require(ModelDescriptionConstants.INTERFACE).asString();
        final int port = operation.require(ModelDescriptionConstants.PORT).asInt();

        model.get(ModelDescriptionConstants.INTERFACE).set(interfaceName);
        model.get(ModelDescriptionConstants.PORT).set(port);
    }

    protected void performRuntime(NewOperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final String interfaceName = operation.require(ModelDescriptionConstants.INTERFACE).asString();
        final int port = operation.require(ModelDescriptionConstants.PORT).asInt();


        final ServiceTarget serviceTarget = context.getServiceTarget();

        Logger.getLogger("org.jboss.as").infof("creating native management service using network interface (%s) port (%s)", interfaceName, port);

        final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("host", "controller");
        final ServiceName threadFactoryServiceName = SERVICE_NAME_BASE.append("thread-factory");
        final ServiceName executorServiceName = SERVICE_NAME_BASE.append("executor");

        // Add the management communication service
        final ManagementCommunicationService managementCommunicationService = new ManagementCommunicationService();
        newControllers.add(serviceTarget.addService(ManagementCommunicationService.SERVICE_NAME, managementCommunicationService)
                .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(interfaceName), NetworkInterfaceBinding.class, managementCommunicationService.getInterfaceInjector())
                .addInjection(managementCommunicationService.getPortInjector(), port)
                .addDependency(executorServiceName, ExecutorService.class, managementCommunicationService.getExecutorServiceInjector())
                .addDependency(threadFactoryServiceName, ThreadFactory.class, managementCommunicationService.getThreadFactoryInjector())
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install());

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ManagementDescription.getAddNativeManagementDescription(locale);
    }

}
