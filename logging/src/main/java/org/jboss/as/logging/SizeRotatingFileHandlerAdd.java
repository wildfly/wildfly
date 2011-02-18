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

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.logging.CommonAttributes.AUTOFLUSH;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FILE;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.HANDLER_TYPE;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.MAX_BACKUP_INDEX;
import static org.jboss.as.logging.CommonAttributes.PATH;
import static org.jboss.as.logging.CommonAttributes.RELATIVE_TO;
import static org.jboss.as.logging.CommonAttributes.ROTATE_SIZE;

import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.services.path.AbstractPathService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
class SizeRotatingFileHandlerAdd implements ModelAddOperationHandler {

    static final SizeRotatingFileHandlerAdd INSTANCE = new SizeRotatingFileHandlerAdd();

    static final String OPERATION_NAME = "add-size-periodic-handler";

    static long DEFAULT_ROTATE_SIZE = 2L * 1024L * 1024L;

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));
        compensatingOperation.get(OP).set(REMOVE);

        final String handlerType = operation.require(HANDLER_TYPE).asString();
        final LoggerHandlerType type = LoggerHandlerType.valueOf(handlerType);
        if(type != LoggerHandlerType.SIZE_ROTATING_FILE_HANDLER) {
            throw new OperationFailedException(new ModelNode().set("invalid operation for handler-type: " + type));
        }

        final ModelNode subModel = context.getSubModel();
        subModel.get(AUTOFLUSH).set(operation.get(AUTOFLUSH));
        subModel.get(ENCODING).set(operation.get(ENCODING));
        subModel.get(FORMATTER).set(operation.get(FORMATTER));
        subModel.get(HANDLER_TYPE).set(handlerType);
        subModel.get(LEVEL).set(operation.get(LEVEL));
        subModel.get(FILE).set(operation.get(FILE));
        subModel.get(MAX_BACKUP_INDEX).set(operation.get(MAX_BACKUP_INDEX));
        subModel.get(ROTATE_SIZE).set(operation.get(ROTATE_SIZE));

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceTarget serviceTarget = context.getServiceTarget();
                    try {
                        final SizeRotatingFileHandlerService service = new SizeRotatingFileHandlerService();
                        final ServiceBuilder<Handler> serviceBuilder = serviceTarget.addService(LogServices.handlerName(name), service);
                        if (operation.has(FILE)) {
                            if (operation.get(FILE).has(RELATIVE_TO)) {
                                serviceBuilder.addDependency(AbstractPathService.pathNameOf(operation.get(FILE, RELATIVE_TO).asString()), String.class, service.getRelativeToInjector());
                            }
                            service.setPath(operation.get(FILE, PATH).asString());
                        }
                        service.setLevel(Level.parse(operation.get(LEVEL).asString()));
                        final Boolean autoFlush = operation.get(AUTOFLUSH).asBoolean();
                        if (autoFlush != null) service.setAutoflush(autoFlush.booleanValue());
                        if (operation.has(ENCODING)) service.setEncoding(operation.get(ENCODING).asString());
                        if (operation.has(FORMATTER)) service.setFormatterSpec(createFormatterSpec(operation));
                        if (operation.has(MAX_BACKUP_INDEX))
                            service.setMaxBackupIndex(operation.get(MAX_BACKUP_INDEX).asInt());
                        if (operation.has(ROTATE_SIZE))
                            service.setRotateSize(operation.get(ROTATE_SIZE).asLong(DEFAULT_ROTATE_SIZE));
                        serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
                        serviceBuilder.addListener(new ResultHandler.ServiceStartListener(resultHandler));
                        serviceBuilder.install();
                    } catch (Throwable t) {
                        throw new OperationFailedException(new ModelNode().set(t.getLocalizedMessage()));
                    }
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensatingOperation);
    }

    static AbstractFormatterSpec createFormatterSpec(final ModelNode operation) {
        return new PatternFormatterSpec(operation.get(FORMATTER).asString());
    }
}
