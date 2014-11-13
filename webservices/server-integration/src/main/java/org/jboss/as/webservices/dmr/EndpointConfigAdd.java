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
package org.jboss.as.webservices.dmr;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.webservices.dmr.PackageUtils.getEndpointConfigServiceName;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.webservices.service.ConfigService;
import org.jboss.as.webservices.service.PropertyService;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;

/**
 * @author <a href="ema@redhat.com">Jim Ma</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
final class EndpointConfigAdd extends AbstractAddStepHandler {

    static final EndpointConfigAdd INSTANCE = new EndpointConfigAdd();

    private EndpointConfigAdd() {
        // forbidden instantiation
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        // does nothing
    }

    @Override
    protected void rollbackRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final List<ServiceController<?>> controllers) {
        super.rollbackRuntime(context, operation, model, controllers);
        if (!context.isBooting()) {
           context.revertReloadRequired();
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        //modify the runtime if we're booting, otherwise set reload required and leave the runtime unchanged
        if (context.isBooting()) {
           final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
           final String name = address.getLastElement().getValue();

           //get the server config object from the ServerConfigService (service installed but not started yet, but the object is fine for our needs here)
           final ServerConfig serverConfig = ASHelper.getMSCService(WSServices.CONFIG_SERVICE, ServerConfig.class, context);
           final ServiceName serviceName = getEndpointConfigServiceName(name);
           final ConfigService endpointConfigService = new ConfigService(serverConfig, name, false);

           final ServiceTarget target = context.getServiceTarget();
           final ServiceBuilder<?> serviceBuilder = target.addService(serviceName, endpointConfigService);
           for (ServiceName sn : PackageUtils.getServiceNameDependencies(context, serviceName, address, Constants.PROPERTY)) {
               serviceBuilder.addDependency(sn, PropertyService.class, endpointConfigService.getPropertiesInjector()); //get a new injector instance each time
           }
           for (ServiceName sn : PackageUtils.getServiceNameDependencies(context, serviceName, address, Constants.PRE_HANDLER_CHAIN)) {
               serviceBuilder.addDependency(sn, UnifiedHandlerChainMetaData.class, endpointConfigService.getPreHandlerChainsInjector()); //get a new injector instance each time
           }
           for (ServiceName sn : PackageUtils.getServiceNameDependencies(context, serviceName, address, Constants.POST_HANDLER_CHAIN)) {
               serviceBuilder.addDependency(sn, UnifiedHandlerChainMetaData.class, endpointConfigService.getPostHandlerChainsInjector()); //get a new injector instance each time
           }
           ServiceController<?> controller = serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();
           if (newControllers != null) {
               newControllers.add(controller);
           }
        } else {
           context.reloadRequired();
        }
    }
}
