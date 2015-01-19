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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.webservices.dmr.Constants.HANDLER;
import static org.jboss.as.webservices.dmr.Constants.PROTOCOL_BINDINGS;
import static org.jboss.as.webservices.dmr.PackageUtils.getConfigServiceName;
import static org.jboss.as.webservices.dmr.PackageUtils.getHandlerChainServiceName;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.service.HandlerChainService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
final class HandlerChainAdd extends AbstractAddStepHandler {

    static final HandlerChainAdd INSTANCE = new HandlerChainAdd();

    private HandlerChainAdd() {
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
            final String protocolBindings = getAttributeValue(operation, PROTOCOL_BINDINGS);
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            final PathElement confElem = address.getElement(address.size() - 2);
            final String configType = confElem.getKey();
            final String configName = confElem.getValue();
            final String handlerChainType = address.getElement(address.size() - 1).getKey();
            final String handlerChainId = address.getElement(address.size() - 1).getValue();

            final ServiceName configServiceName = getConfigServiceName(configType, configName);
            if (context.getServiceRegistry(false).getService(configServiceName) == null) {
                throw WSLogger.ROOT_LOGGER.missingConfig(configName);
            }

            final ServiceName handlerChainServiceName = getHandlerChainServiceName(configServiceName, handlerChainType, handlerChainId);
            final HandlerChainService service = new HandlerChainService(handlerChainType, handlerChainId, protocolBindings);
            final ServiceTarget target = context.getServiceTarget();
            final ServiceBuilder<?> handlerChainServiceBuilder = target.addService(handlerChainServiceName, service);
            for (ServiceName sn : PackageUtils.getServiceNameDependencies(context, handlerChainServiceName, address, HANDLER)) {
                handlerChainServiceBuilder.addDependency(sn, UnifiedHandlerMetaData.class, service.getHandlersInjector()); //get a new injector instance each time
            }
            handlerChainServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();
        } else {
            context.reloadRequired();
        }
    }

    private static String getAttributeValue(final ModelNode node, final String propertyName) {
        return node.hasDefined(propertyName) ? node.get(propertyName).asString() : null;
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        Attributes.PROTOCOL_BINDINGS.validateAndSet(operation, model);
    }
}
