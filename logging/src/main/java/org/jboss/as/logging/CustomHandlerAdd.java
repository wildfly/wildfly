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
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.logging.CommonAttributes.CLASS;
import static org.jboss.as.logging.CommonAttributes.CUSTOM_HANDLER;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.MODULE;
import static org.jboss.as.logging.CommonAttributes.PROPERTIES;

/**
 * Date: 03.08.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class CustomHandlerAdd extends AbstractAddStepHandler {

    static final CustomHandlerAdd INSTANCE = new CustomHandlerAdd();

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        LoggingValidators.validate(operation);
        model.get(ENCODING).set(operation.get(ENCODING));
        model.get(FORMATTER).set(operation.get(FORMATTER));
        if (operation.hasDefined(LEVEL)) model.get(LEVEL).set(operation.get(LEVEL));
        model.get(MODULE).set(operation.get(MODULE));
        model.get(CLASS).set(operation.get(CLASS));
        if (operation.hasDefined(PROPERTIES)) model.get(PROPERTIES).set(operation.get(PROPERTIES));
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final String className = operation.get(CLASS).asString();
        final String moduleName = operation.get(MODULE).asString();
        final CustomHandlerService service = new CustomHandlerService(className, moduleName);
        final ServiceBuilder<Handler> serviceBuilder = serviceTarget.addService(LogServices.handlerName(name), service);
        try {
            if (operation.hasDefined(LEVEL)) service.setLevel(Level.parse(operation.get(LEVEL).asString()));
            if (operation.hasDefined(ENCODING)) service.setEncoding(operation.get(ENCODING).asString());
        } catch (Throwable t) {
            throw new OperationFailedException(new ModelNode().set(t.getLocalizedMessage()));
        }
        if (operation.hasDefined(PROPERTIES)) service.addProperties(operation.get(PROPERTIES).asPropertyList());
        service.setFormatterSpec(AbstractFormatterSpec.Factory.create(operation));
        serviceBuilder.addListener(verificationHandler);
        serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        newControllers.add(serviceBuilder.install());
    }
}
