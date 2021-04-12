/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.webservices.dmr.PackageUtils.getClientConfigServiceName;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.webservices.service.ConfigService;
import org.jboss.as.webservices.service.PropertyService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.wsf.spi.metadata.config.AbstractCommonConfig;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
final class ClientConfigAdd extends AbstractAddStepHandler {

    static final ClientConfigAdd INSTANCE = new ClientConfigAdd();

    private ClientConfigAdd() {
        // forbidden instantiation
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) {
        // does nothing
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, Resource resource) {
        super.rollbackRuntime(context, operation, resource);
        if (!context.isBooting()) {
            context.revertReloadRequired();
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
      //modify the runtime if we're booting, otherwise set reload required and leave the runtime unchanged
      if (context.isBooting()) {
         final PathAddress address = context.getCurrentAddress();
         final String name = context.getCurrentAddressValue();
         final ServiceName serviceName = getClientConfigServiceName(name);
         final ServiceTarget target = context.getServiceTarget();
         final ServiceBuilder<?> clientServiceBuilder = target.addService(serviceName);
         final List<Supplier<PropertyService>> propertySuppliers = new ArrayList<>();
         for (ServiceName sn : PackageUtils.getServiceNameDependencies(context, serviceName, address, Constants.PROPERTY)) {
             propertySuppliers.add(clientServiceBuilder.requires(sn));
         }
         final List<Supplier<UnifiedHandlerChainMetaData>> preHandlerChainSuppliers = new ArrayList<>();
         for (ServiceName sn : PackageUtils.getServiceNameDependencies(context, serviceName, address, Constants.PRE_HANDLER_CHAIN)) {
             preHandlerChainSuppliers.add(clientServiceBuilder.requires(sn));
         }
         final List<Supplier<UnifiedHandlerChainMetaData>> postHandlerChainSuppliers = new ArrayList<>();
         for (ServiceName sn : PackageUtils.getServiceNameDependencies(context, serviceName, address, Constants.POST_HANDLER_CHAIN)) {
             postHandlerChainSuppliers.add(clientServiceBuilder.requires(sn));
         }
         final Consumer<AbstractCommonConfig> config = clientServiceBuilder.provides(serviceName);
         clientServiceBuilder.setInstance(new ConfigService(name, true, config, propertySuppliers, preHandlerChainSuppliers, postHandlerChainSuppliers));
         clientServiceBuilder.install();
      } else {
         context.reloadRequired();
      }
    }

}
