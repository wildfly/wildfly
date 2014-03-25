/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.io.File;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.io.IOServices;
import org.xnio.XnioWorker;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class AccessLogAdd extends AbstractAddStepHandler {

    AccessLogAdd() {
        super(AccessLogDefinition.ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final PathAddress hostAddress = address.subAddress(0, address.size() - 1);
        final PathAddress serverAddress = hostAddress.subAddress(0, hostAddress.size() - 1);
        final String worker = AccessLogDefinition.WORKER.resolveModelAttribute(context, model).asString();
        final String pattern = AccessLogDefinition.PATTERN.resolveModelAttribute(context, model).asString();
        final String directory = AccessLogDefinition.DIRECTORY.resolveModelAttribute(context, model).asString();
        final String filePrefix = AccessLogDefinition.PREFIX.resolveModelAttribute(context, model).asString();
        final String fileSuffix = AccessLogDefinition.SUFFIX.resolveModelAttribute(context, model).asString();


        final AccessLogService service = new AccessLogService(pattern, new File(directory), filePrefix, fileSuffix);
        final String serverName = serverAddress.getLastElement().getValue();
        final String hostName = hostAddress.getLastElement().getValue();

        final ServiceName serviceName = UndertowService.accessLogServiceName(serverName, hostName);
        final ServiceBuilder<AccessLogService> builder = context.getServiceTarget().addService(serviceName, service)
                .addDependency(IOServices.WORKER.append(worker), XnioWorker.class, service.getWorker());

        builder.setInitialMode(ServiceController.Mode.ACTIVE);
        builder.addListener(verificationHandler);
        final ServiceController<AccessLogService> serviceController = builder.install();
        if (newControllers != null) {
            newControllers.add(serviceController);
        }
    }
}
