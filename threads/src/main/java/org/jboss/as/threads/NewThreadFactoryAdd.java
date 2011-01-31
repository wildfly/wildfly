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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.threads.CommonAttributes.GROUP_NAME;
import static org.jboss.as.threads.CommonAttributes.PRIORITY;
import static org.jboss.as.threads.CommonAttributes.PROPERTIES;
import static org.jboss.as.threads.CommonAttributes.THREAD_NAME_PATTERN;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class NewThreadFactoryAdd implements RuntimeOperationHandler, ModelAddOperationHandler {

    static final OperationHandler INSTANCE = new NewThreadFactoryAdd();

    @Override
    public Cancellable execute(final NewOperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
        //Get/validate the properties
        final String name = operation.require(NAME).asString();
        final String groupName = has(operation, GROUP_NAME) ? operation.get(GROUP_NAME).asString() : null;
        final String threadNamePattern = has(operation, THREAD_NAME_PATTERN) ? operation.get(THREAD_NAME_PATTERN).asString() : null;
        final int priority = has(operation, PRIORITY) ? operation.get(PRIORITY).asInt() : -1;
        if (priority != -1 && priority < 0 || priority > 10) {
            throw new IllegalArgumentException(PRIORITY + " is out of range " + priority); //TODO i18n
        }
        final ModelNode properties = has(operation, PROPERTIES) ? operation.get(PROPERTIES) : null;

        if (context instanceof NewRuntimeOperationContext) {
            final NewRuntimeOperationContext runtimeContext = (NewRuntimeOperationContext) context;
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
        final ModelNode compensating = new ModelNode();
        compensating.get(OP_ADDR).set(operation.require(ADDRESS));
        compensating.get(OP).set(REMOVE);
        compensating.get(NAME).set(name);
        resultHandler.handleResultComplete(compensating);

        return Cancellable.NULL;
    }

    private boolean has(ModelNode operation, String name) {
        return operation.has(name) && operation.get(name).isDefined();
    }

}
