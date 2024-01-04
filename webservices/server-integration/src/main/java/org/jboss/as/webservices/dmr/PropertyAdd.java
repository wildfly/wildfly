/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.dmr;

import static org.jboss.as.webservices.dmr.PackageUtils.getConfigServiceName;
import static org.jboss.as.webservices.dmr.PackageUtils.getPropertyServiceName;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.service.PropertyService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import java.util.function.Consumer;

/**
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class PropertyAdd extends AbstractAddStepHandler {

    static final PropertyAdd INSTANCE = new PropertyAdd();

    private PropertyAdd() {
        // forbidden instantiation
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
            final String propertyName = context.getCurrentAddressValue();
            final PathElement confElem = address.getElement(address.size() - 2);
            final String configType = confElem.getKey();
            final String configName = confElem.getValue();
            final String propertyValue = Attributes.VALUE.resolveModelAttribute(context, model).asStringOrNull();
            final ServiceTarget target = context.getServiceTarget();
            final ServiceName configServiceName = getConfigServiceName(configType, configName);
            if (context.getServiceRegistry(false).getService(configServiceName) == null) {
                throw WSLogger.ROOT_LOGGER.missingConfig(configName);
            }
            final ServiceName propertyServiceName = getPropertyServiceName(configServiceName, propertyName);
            final ServiceBuilder<?> propertyServiceBuilder = target.addService(propertyServiceName);
            final Consumer<PropertyService> propertyServiceConsumer = propertyServiceBuilder.provides(propertyServiceName);
            propertyServiceBuilder.setInstance(new PropertyService(propertyName, propertyValue, propertyServiceConsumer));
            propertyServiceBuilder.install();
        } else {
            context.reloadRequired();
        }
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        Attributes.VALUE.validateAndSet(operation,model);
    }
}
