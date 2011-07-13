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

import java.util.Collection;
import java.util.logging.Level;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import static org.jboss.as.logging.CommonAttributes.HANDLERS;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.ROOT_LOGGER;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
class RootLoggerAdd implements OperationStepHandler {

    static final RootLoggerAdd INSTANCE = new RootLoggerAdd();

    static final String OPERATION_NAME = "set-root-logger";

    public void execute(OperationContext context, ModelNode operation) {
        final String level = operation.require(LEVEL).asString();
        final ModelNode handlers = operation.get(HANDLERS);

        final ModelNode subModel = context.readModelForUpdate(PathAddress.EMPTY_ADDRESS);
        subModel.get(ROOT_LOGGER, LEVEL).set(level);
        subModel.get(ROOT_LOGGER, HANDLERS).set(handlers);

        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    LoggingValidators.validate(operation);
                    final ServiceTarget target = context.getServiceTarget();
                    final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                    try {

                        final RootLoggerService service = new RootLoggerService();
                        if (operation.hasDefined(LEVEL)) service.setLevel(Level.parse(level));
                        target.addService(LogServices.ROOT_LOGGER, service)
                                .addListener(verificationHandler)
                                .setInitialMode(ServiceController.Mode.ACTIVE)
                                .install();


                    } catch (Throwable t) {
                        throw new OperationFailedException(new ModelNode().set(t.getLocalizedMessage()));
                    }
                    Collection<ServiceController<?>> loggerControllers = null;
                    try {
                        // install logger handler services
                        if (handlers.getType() != ModelType.UNDEFINED) {
                            loggerControllers = LogServices.installLoggerHandlers(target, "", handlers, verificationHandler);
                        }
                    } catch (Throwable t) {
                        throw new OperationFailedException(new ModelNode().set(t.getLocalizedMessage()));
                    }

                    context.addStep(verificationHandler, OperationContext.Stage.VERIFY);

                    if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        context.removeService(LogServices.ROOT_LOGGER);
                        if (loggerControllers != null) for (ServiceController<?> loggerController : loggerControllers) {
                            context.removeService(loggerController.getName());
                        }
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }
}
