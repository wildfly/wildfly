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

package org.jboss.as.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.OVERFLOW_ACTION;
import static org.jboss.as.logging.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.logging.CommonAttributes.SUBHANDLERS;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
class AsyncHandlerAdd extends AbstractAddStepHandler {

    static final AsyncHandlerAdd INSTANCE = new AsyncHandlerAdd();

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.get(QUEUE_LENGTH).set(operation.get(QUEUE_LENGTH));
        model.get(SUBHANDLERS).set(operation.get(SUBHANDLERS));
        model.get(LEVEL).set(operation.get(LEVEL));
        model.get(OVERFLOW_ACTION).set(operation.get(OVERFLOW_ACTION));
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final AsyncHandlerService service = new AsyncHandlerService();
        final ServiceBuilder<Handler> serviceBuilder = serviceTarget.addService(LogServices.handlerName(name), service);
        final List<InjectedValue<Handler>> list = new ArrayList<InjectedValue<Handler>>();
        if (operation.hasDefined(SUBHANDLERS)) for (final ModelNode handlerName : operation.get(SUBHANDLERS).asList()) {
            final InjectedValue<Handler> injectedValue = new InjectedValue<Handler>();
            serviceBuilder.addDependency(LogServices.handlerName(handlerName.asString()), Handler.class, injectedValue);
            list.add(injectedValue);
        }
        service.addHandlers(list);
        if (operation.hasDefined(QUEUE_LENGTH))
            service.setQueueLength(operation.get(QUEUE_LENGTH).asInt());
        service.setLevel(Level.parse(operation.get(LEVEL).asString()));
        service.setOverflowAction(OverflowAction.valueOf(operation.get(OVERFLOW_ACTION).asString()));

        serviceBuilder.addListener(verificationHandler);
        serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        newControllers.add(serviceBuilder.install());
    }

}
