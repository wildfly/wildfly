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

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.Services;
import org.jboss.as.server.mgmt.HttpManagementService;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.as.server.services.path.AbstractPathService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.threads.JBossThreadFactory;

import java.security.AccessController;
import java.util.concurrent.Executors;

/**
 * {@code OperationStepHandler} for changing attributes on the http management interface.
 *
 * @author Emanuel Muckenhuber
 */
public class HttpManagementAttributeHandlers {

    public static final OperationStepHandler INSTANCE = new HttpManagementAttributeHandler();

    private HttpManagementAttributeHandlers() {
        //
    }


    static class HttpManagementAttributeHandler extends WriteAttributeHandlers.WriteAttributeOperationHandler {

        @Override
        protected void modelChanged(final OperationContext context, final ModelNode operation, String attributeName, ModelNode newValue, ModelNode currentValue) throws OperationFailedException {
            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            final ModelNode subModel = resource.getModel();
            if(! newValue.equals(currentValue)) {
                context.addStep(new OperationStepHandler(){
                    @Override
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                        context.removeService(HttpManagementService.SERVICE_NAME);
                        final ServiceVerificationHandler handler = new ServiceVerificationHandler();
                        addService(context.getServiceTarget(), subModel, handler);
                        context.addStep(handler, OperationContext.Stage.VERIFY);
                        context.completeStep();
                    }
                }, OperationContext.Stage.RUNTIME);
            }
            context.completeStep();
        }
    }

    static void addService(final ServiceTarget serviceTarget, final ModelNode subModel, final ServiceVerificationHandler handler) {
        final String interfaceName = subModel.require(ModelDescriptionConstants.INTERFACE).asString();
        final int port = getIntValue(subModel, ModelDescriptionConstants.PORT);
        final int securePort = getIntValue(subModel, ModelDescriptionConstants.SECURE_PORT);
        final String securityRealm = subModel.hasDefined(SECURITY_REALM) ? subModel.get(SECURITY_REALM).asString() : null;

        final HttpManagementService service = new HttpManagementService();
        ServiceBuilder builder = serviceTarget.addService(HttpManagementService.SERVICE_NAME, service)
                .addDependency(
                        NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(interfaceName),
                        NetworkInterfaceBinding.class, service.getInterfaceInjector())
                .addListener(handler)
                .addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, service.getModelControllerInjector())
                .addDependency(AbstractPathService.pathNameOf(ServerEnvironment.SERVER_TEMP_DIR), String.class, service.getTempDirInjector())
                .addInjection(service.getPortInjector(), port)
                .addInjection(service.getSecurePortInjector(), securePort)
                .addInjection(service.getExecutorServiceInjector(), Executors.newCachedThreadPool(new JBossThreadFactory(new ThreadGroup("HttpManagementService-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext())));

        if (securityRealm != null) {
            builder.addDependency(SecurityRealmService.BASE_SERVICE_NAME.append(securityRealm), SecurityRealmService.class, service.getSecurityRealmInjector());
        }
        builder.setInitialMode(ServiceController.Mode.ACTIVE)
               .install();
    }

    static int getIntValue(ModelNode source, String name) {
        if (source.has(name)) {
            return source.require(name).asInt();
        }
        return -1;
    }

}
