/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.dmr;

import static org.jboss.as.webservices.dmr.PackageUtils.getConfigServiceName;
import static org.jboss.as.webservices.dmr.PackageUtils.getHandlerChainServiceName;
import static org.jboss.as.webservices.dmr.PackageUtils.getHandlerServiceName;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.service.HandlerService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData;

/**
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class HandlerAdd extends AbstractAddStepHandler {

    static final HandlerAdd INSTANCE = new HandlerAdd();
    static final AtomicInteger counter = new AtomicInteger(0);

    private HandlerAdd() {
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
        // modify the runtime if we're booting, otherwise set reload required and leave the runtime unchanged
        if (context.isBooting()) {
            final PathAddress address = context.getCurrentAddress();
            final PathElement confElem = address.getElement(address.size() - 3);
            final String configType = confElem.getKey();
            final String configName = confElem.getValue();
            final String handlerChainType = address.getElement(address.size() - 2).getKey();
            final String handlerChainId = address.getElement(address.size() - 2).getValue();
            final String handlerName = address.getElement(address.size() - 1).getValue();
            final String handlerClass = Attributes.CLASS.resolveModelAttribute(context, model).asString();

            final ServiceTarget target = context.getServiceTarget();
            final ServiceName configServiceName = getConfigServiceName(configType, configName);
            final ServiceRegistry registry = context.getServiceRegistry(false);
            if (registry.getService(configServiceName) == null) {
                throw WSLogger.ROOT_LOGGER.missingConfig(configName);
            }
            final ServiceName handlerChainServiceName = getHandlerChainServiceName(configServiceName, handlerChainType, handlerChainId);
            if (registry.getService(handlerChainServiceName) == null) {
                throw WSLogger.ROOT_LOGGER.missingHandlerChain(configName, handlerChainType, handlerChainId);
            }
            final ServiceName handlerServiceName = getHandlerServiceName(handlerChainServiceName, handlerName);
            final ServiceBuilder<?> handlerServiceBuilder = target.addService(handlerServiceName);
            final Consumer<UnifiedHandlerMetaData> handlerConsumer = handlerServiceBuilder.provides(handlerServiceName);
            handlerServiceBuilder.setInstance(new HandlerService(handlerName, handlerClass, counter.incrementAndGet(), handlerConsumer));
            handlerServiceBuilder.install();
        } else {
            context.reloadRequired();
        }
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        Attributes.CLASS.validateAndSet(operation, model);
    }
}
