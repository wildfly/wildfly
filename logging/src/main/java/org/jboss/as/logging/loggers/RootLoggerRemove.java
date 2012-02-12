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

package org.jboss.as.logging.loggers;

import static org.jboss.as.logging.CommonAttributes.ROOT_LOGGER_NAME;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.logging.CommonAttributes;
import org.jboss.as.logging.util.LogServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
public class RootLoggerRemove extends AbstractRemoveStepHandler {

    public static final RootLoggerRemove INSTANCE = new RootLoggerRemove();

    public static final String OPERATION_NAME = "remove";
    public static final String LEGACY_OPERATION_NAME = "remove-root-logger";

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.HANDLERS.getName())) {
            for (ModelNode handler : model.get(CommonAttributes.HANDLERS.getName()).asList()) {
                context.removeService(LogServices.loggerHandlerName(ROOT_LOGGER_NAME, handler.asString()));
            }
        }
        context.removeService(LogServices.ROOT_LOGGER);
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final List<ServiceController<?>> controllers = new ArrayList<ServiceController<?>>();
        final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
        RootLoggerAdd.INSTANCE.performRuntime(context, operation, model, verificationHandler, controllers);
    }
}
