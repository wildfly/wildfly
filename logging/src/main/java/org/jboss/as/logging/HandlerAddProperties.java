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

package org.jboss.as.logging;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.logging.CommonAttributes.COMMON_HANDLER_ATTRIBUTES;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.LEVEL;

/**
 * Date: 23.09.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class HandlerAddProperties<T extends HandlerService> extends AbstractAddStepHandler {

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        ENCODING.validateAndSet(operation, model);
        FORMATTER.validateAndSet(operation, model);
        LEVEL.validateAndSet(operation, model);
    }

    @Override
    protected final void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                        final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final T service = createHandlerService(model);
        final ServiceBuilder<Handler> serviceBuilder = serviceTarget.addService(LogServices.handlerName(name), service);
        final ModelNode level = LEVEL.validateOperation(model);
        final ModelNode encoding = ENCODING.validateOperation(model);
        final ModelNode formatter = FORMATTER.validateOperation(model);

        if (level.isDefined()) {
            service.setLevel(Level.parse(level.asString()));
        }

        try {
            if (encoding.isDefined())
                service.setEncoding(encoding.asString());
        } catch (Throwable t) {
            throw new OperationFailedException(new ModelNode().set(t.getLocalizedMessage()));
        }

        if (formatter.isDefined()) {
            service.setFormatterSpec(AbstractFormatterSpec.fromModelNode(model));
        }

        updateRuntime(context, serviceBuilder, name, service, model);

        serviceBuilder.addListener(verificationHandler);
        serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        newControllers.add(serviceBuilder.install());
    }

    protected abstract T createHandlerService(ModelNode model) throws OperationFailedException;

    protected abstract void updateRuntime(final OperationContext context, final ServiceBuilder<?> serviceBuilder, final String name, final T service, final ModelNode model) throws OperationFailedException;
}
