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

import java.util.List;
import java.util.logging.Level;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
class LoggerAdd extends AbstractAddStepHandler {

    static final LoggerAdd INSTANCE = new LoggerAdd();

    protected void populateModel(ModelNode operation, ModelNode model) {
        final String level = operation.require(CommonAttributes.LEVEL).asString();
        final ModelNode handlers = operation.hasDefined(CommonAttributes.HANDLERS) ? operation.get(CommonAttributes.HANDLERS) : new ModelNode();

        model.get(CommonAttributes.LEVEL).set(level);
        model.get(CommonAttributes.HANDLERS).set(handlers);
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final String level = operation.require(CommonAttributes.LEVEL).asString();
        final ModelNode handlers = operation.hasDefined(CommonAttributes.HANDLERS) ? operation.get(CommonAttributes.HANDLERS) : new ModelNode();

        final ServiceTarget target = context.getServiceTarget();
        final String loggerName = name;
        try {
            // Install logger service
            final LoggerService service = new LoggerService(loggerName);
            service.setLevel(Level.parse(level));
            newControllers.add(target.addService(LogServices.loggerName(loggerName), service)
                    .addListener(verificationHandler)
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install());
        } catch (Throwable t) {
            throw new OperationFailedException(new ModelNode().set(t.getLocalizedMessage()));
        }
        try {
            // install logger handler services
            if (handlers.isDefined()) {
                newControllers.addAll(LogServices.installLoggerHandlers(target, loggerName, handlers, verificationHandler));
            }
        } catch (Throwable t) {
            throw new OperationFailedException(new ModelNode().set(t.getLocalizedMessage()));
        }
    }
}
