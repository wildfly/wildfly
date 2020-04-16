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

package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.ejb3.timerservice.persistence.TimerPersistence;
import org.jboss.as.ejb3.timerservice.persistence.filestore.FileTimerPersistence;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * Adds the timer service file based data store
 *
 * @author Stuart Douglas
 */
public class FileDataStoreAdd extends AbstractAddStepHandler {

    FileDataStoreAdd(AttributeDefinition... attributes) {
        super(attributes);
    }

    protected void performRuntime(final OperationContext context, ModelNode operation, final ModelNode model) throws OperationFailedException {
        final ModelNode pathNode = FileDataStoreResourceDefinition.PATH.resolveModelAttribute(context, model);
        final String path = pathNode.isDefined() ? pathNode.asString() : null;
        final ModelNode relativeToNode = FileDataStoreResourceDefinition.RELATIVE_TO.resolveModelAttribute(context, model);
        final String relativeTo = relativeToNode.isDefined() ? relativeToNode.asString() : null;

        final FileTimerPersistence fileTimerPersistence = new FileTimerPersistence(true, path, relativeTo);
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final ServiceName serviceName = TimerPersistence.SERVICE_NAME.append(address.getLastElement().getValue());
        final ServiceBuilder sb = context.getServiceTarget().addService(serviceName, fileTimerPersistence);
        sb.addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, fileTimerPersistence.getModuleLoader());
        sb.addDependency(PathManagerService.SERVICE_NAME, PathManager.class, fileTimerPersistence.getPathManager());
        sb.requires(context.getCapabilityServiceName("org.wildfly.transactions.global-default-local-provider", null));
        sb.addDependency(context.getCapabilityServiceName("org.wildfly.transactions.transaction-synchronization-registry", null), TransactionSynchronizationRegistry.class, fileTimerPersistence.getTransactionSynchronizationRegistry());
        sb.install();
    }

}
