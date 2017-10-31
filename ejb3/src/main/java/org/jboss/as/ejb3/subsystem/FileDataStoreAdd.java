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

import javax.transaction.TransactionManager;
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
import org.jboss.as.txn.service.TransactionManagerService;
import org.jboss.as.txn.service.TransactionSynchronizationRegistryService;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceName;

/**
 * Adds the timer service file based data store
 *
 * @author Stuart Douglas
 */
public class FileDataStoreAdd extends AbstractAddStepHandler {

    public static final FileDataStoreAdd INSTANCE = new FileDataStoreAdd();

    protected void populateModel(ModelNode operation, ModelNode timerServiceModel) throws OperationFailedException {

        for (AttributeDefinition attr : FileDataStoreResourceDefinition.ATTRIBUTES.values()) {
            attr.validateAndSet(operation, timerServiceModel);
        }
    }


    protected void performRuntime(final OperationContext context, ModelNode operation, final ModelNode model) throws OperationFailedException {

        final ModelNode pathNode = FileDataStoreResourceDefinition.PATH.resolveModelAttribute(context, model);
        final String path = pathNode.isDefined() ? pathNode.asString() : null;
        final ModelNode relativeToNode = FileDataStoreResourceDefinition.RELATIVE_TO.resolveModelAttribute(context, model);
        final String relativeTo = relativeToNode.isDefined() ? relativeToNode.asString() : null;


        final FileTimerPersistence fileTimerPersistence = new FileTimerPersistence(true, path, relativeTo);
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final ServiceName serviceName = TimerPersistence.SERVICE_NAME.append(address.getLastElement().getValue());
        context.getServiceTarget().addService(serviceName, fileTimerPersistence)
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, fileTimerPersistence.getModuleLoader())
                .addDependency(PathManagerService.SERVICE_NAME, PathManager.class, fileTimerPersistence.getPathManager())
                .addDependency(TransactionManagerService.SERVICE_NAME, TransactionManager.class, fileTimerPersistence.getTransactionManager())
                .addDependency(TransactionSynchronizationRegistryService.SERVICE_NAME, TransactionSynchronizationRegistry.class, fileTimerPersistence.getTransactionSynchronizationRegistry())
                .install();

    }

}
