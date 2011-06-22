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
import java.util.logging.Handler;
import java.util.logging.Level;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.logging.CommonAttributes.AUTOFLUSH;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FILE;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.PATH;
import static org.jboss.as.logging.CommonAttributes.RELATIVE_TO;
import org.jboss.as.server.services.path.AbstractPathService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
class FileHandlerAdd extends AbstractAddStepHandler {

    static final FileHandlerAdd INSTANCE = new FileHandlerAdd();

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.get(AUTOFLUSH).set(operation.get(AUTOFLUSH));
        model.get(ENCODING).set(operation.get(ENCODING));
        model.get(FORMATTER).set(operation.get(FORMATTER));
        model.get(LEVEL).set(operation.get(LEVEL));
        model.get(FILE).set(operation.get(FILE));
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        final ServiceTarget serviceTarget = context.getServiceTarget();
        try {
            final FileHandlerService service = new FileHandlerService();

            final ServiceBuilder<Handler> serviceBuilder = serviceTarget.addService(LogServices.handlerName(name), service);
            if (operation.hasDefined(FILE)) {
                final HandlerFileService fileService = new HandlerFileService(operation.get(FILE, PATH).asString());
                final ServiceBuilder<?> fileBuilder = serviceTarget.addService(LogServices.handlerFileName(name), fileService);
                if (operation.get(FILE).hasDefined(CommonAttributes.RELATIVE_TO)) {
                    fileBuilder.addDependency(AbstractPathService.pathNameOf(operation.get(FILE, RELATIVE_TO).asString()), String.class, fileService.getRelativeToInjector());
                }
                fileBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();
                serviceBuilder.addDependency(LogServices.handlerFileName(name), String.class, service.getFileNameInjector());
            }
            service.setLevel(Level.parse(operation.get(LEVEL).asString()));
            final Boolean autoFlush = operation.get(AUTOFLUSH).asBoolean();
            if (autoFlush != null) service.setAutoflush(autoFlush.booleanValue());
            if (operation.hasDefined(ENCODING)) service.setEncoding(operation.get(ENCODING).asString());
            if (operation.hasDefined(FORMATTER)) service.setFormatterSpec(createFormatterSpec(operation));
            serviceBuilder.addListener(verificationHandler);
            serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
            newControllers.add(serviceBuilder.install());
        } catch (Throwable t) {
            throw new OperationFailedException(new ModelNode().set(t.getLocalizedMessage()));
        }

    }

    static AbstractFormatterSpec createFormatterSpec(final ModelNode operation) {
        return new PatternFormatterSpec(operation.get(FORMATTER).asString());
    }
}
