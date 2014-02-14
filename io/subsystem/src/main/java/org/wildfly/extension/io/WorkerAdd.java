/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.io;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.io.WorkerResourceDefinition.WORKER_IO_THREADS;
import static org.wildfly.extension.io.WorkerResourceDefinition.WORKER_TASK_MAX_THREADS;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.xnio.Option;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.XnioWorker;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
class WorkerAdd extends AbstractAddStepHandler {
    static final WorkerAdd INSTANCE = new WorkerAdd();

    private WorkerAdd() {

    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr : WorkerResourceDefinition.ATTRIBUTES) {
            attr.validateAndSet(operation, model);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final OptionMap.Builder builder = OptionMap.builder();

        for (OptionAttributeDefinition attr : WorkerResourceDefinition.ATTRIBUTES) {
            Option option = attr.getOption();
            ModelNode value = attr.resolveModelAttribute(context, model);
            if (!value.isDefined()) {
                continue;
            }
            if (attr.getType() == ModelType.INT) {
                builder.set((Option<Integer>) option, value.asInt());
            } else if (attr.getType() == ModelType.LONG) {
                builder.set(option, value.asLong());
            } else if (attr.getType() == ModelType.BOOLEAN) {
                builder.set(option, value.asBoolean());
            }
        }
        builder.set(Options.WORKER_NAME, name);

        ModelNode ioThreadsModel = WORKER_IO_THREADS.resolveModelAttribute(context, model);
        ModelNode maxTaskThreadsModel = WORKER_TASK_MAX_THREADS.resolveModelAttribute(context, model);
        if (!ioThreadsModel.isDefined()) {
            builder.set((Option<Integer>) WORKER_IO_THREADS.getOption(), Runtime.getRuntime().availableProcessors() * 2);
        }
        if (!maxTaskThreadsModel.isDefined()) {
            builder.set((Option<Integer>) WORKER_TASK_MAX_THREADS.getOption(), Runtime.getRuntime().availableProcessors() * 16);
        }

        final WorkerService workerService = new WorkerService(builder.getMap());
        final ServiceBuilder<XnioWorker> serviceBuilder = context.getServiceTarget().
                addService(IOServices.WORKER.append(name), workerService);

        serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        final ServiceController<XnioWorker> serviceController = serviceBuilder.install();
        if (newControllers != null) {
            newControllers.add(serviceController);
        }
    }
}
