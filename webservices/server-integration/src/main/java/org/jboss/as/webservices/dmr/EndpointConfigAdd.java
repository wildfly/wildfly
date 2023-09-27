/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.dmr;

import static org.jboss.as.webservices.dmr.PackageUtils.getEndpointConfigServiceName;

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
           final ServiceName serviceName = getEndpointConfigServiceName(name);
           final ServiceTarget target = context.getServiceTarget();
           final ServiceBuilder<?> serviceBuilder = target.addService(serviceName);
           final List<Supplier<PropertyService>> propertySuppliers = new ArrayList<>();
           for (ServiceName sn : PackageUtils.getServiceNameDependencies(context, serviceName, address, Constants.PROPERTY)) {
               propertySuppliers.add(serviceBuilder.requires(sn));
           }
           final List<Supplier<UnifiedHandlerChainMetaData>> preHandlerChainSuppliers = new ArrayList<>();
           for (ServiceName sn : PackageUtils.getServiceNameDependencies(context, serviceName, address, Constants.PRE_HANDLER_CHAIN)) {
               preHandlerChainSuppliers.add(serviceBuilder.requires(sn));
           }
           final List<Supplier<UnifiedHandlerChainMetaData>> postHandlerChainSuppliers = new ArrayList<>();
           for (ServiceName sn : PackageUtils.getServiceNameDependencies(context, serviceName, address, Constants.POST_HANDLER_CHAIN)) {
               postHandlerChainSuppliers.add(serviceBuilder.requires(sn));
           }
           final Consumer<AbstractCommonConfig> config = serviceBuilder.provides(serviceName);
           serviceBuilder.setInstance(new ConfigService(name, false, config, propertySuppliers, preHandlerChainSuppliers, postHandlerChainSuppliers));
           serviceBuilder.install();
        } else {
           context.reloadRequired();
        }
    }

}
