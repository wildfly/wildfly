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
package org.jboss.as.threads;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.threads.CommonAttributes.GROUP_NAME;
import static org.jboss.as.threads.CommonAttributes.PRIORITY;
import static org.jboss.as.threads.CommonAttributes.PROPERTIES;
import static org.jboss.as.threads.CommonAttributes.THREAD_NAME_PATTERN;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.RuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;

/**
 * Adds a thread factory to the threads subsystem.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ThreadFactoryAdd implements RuntimeOperationHandler, ModelAddOperationHandler {

    static final OperationHandler INSTANCE = new ThreadFactoryAdd();

    @Override
    public Cancellable execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        final ModelNode opAddr = operation.require(OP_ADDR);
        final PathAddress address = PathAddress.pathAddress(opAddr);
        final String name = address.getLastElement().getValue();

        //Get/validate the properties
        final String groupName = operation.hasDefined(GROUP_NAME) ? operation.get(GROUP_NAME).asString() : null;
        final String threadNamePattern = operation.hasDefined(THREAD_NAME_PATTERN) ? operation.get(THREAD_NAME_PATTERN).asString() : null;
        final int priority = operation.hasDefined(PRIORITY) ? operation.get(PRIORITY).asInt() : -1;
        if (priority != -1 && priority < 0 || priority > 10) {
            throw new IllegalArgumentException(PRIORITY + " is out of range " + priority); //TODO i18n
        }
        final ModelNode properties = operation.hasDefined(PROPERTIES) ? operation.get(PROPERTIES) : null;

        if (context instanceof RuntimeOperationContext) {
            final RuntimeOperationContext runtimeContext = (RuntimeOperationContext) context;
            final ServiceTarget target = runtimeContext.getServiceTarget();
            final ThreadFactoryService service = new ThreadFactoryService();
            service.setNamePattern(threadNamePattern);
            service.setPriority(priority);
            service.setThreadGroupName(groupName);
            //TODO What about the properties?
            //final UpdateResultHandler.ServiceStartListener<P> listener = new UpdateResultHandler.ServiceStartListener<P>(handler, param);
            try {
                target.addService(ThreadsServices.threadFactoryName(name), service)
                    //.addListener(listener)
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
            } catch (ServiceRegistryException e) {
                resultHandler.handleFailed(new ModelNode().set(e.getMessage()));
            }

        }

        //Apply to the model
        final ModelNode model = context.getSubModel();
        model.get(NAME).set(name);
        if (groupName != null) {
            model.get(GROUP_NAME).set(groupName);
        }
        if (threadNamePattern != null) {
            model.get(THREAD_NAME_PATTERN).set(threadNamePattern);
        }
        if (priority >= 0) {
            model.get(PRIORITY).set(priority);
        }
        if (properties != null) {
            model.get(PROPERTIES).set(properties);
        }

        // Compensating is remove
        final ModelNode compensating = Util.getResourceRemoveOperation(opAddr);
        resultHandler.handleResultComplete(compensating);

        return Cancellable.NULL;
    }

}
