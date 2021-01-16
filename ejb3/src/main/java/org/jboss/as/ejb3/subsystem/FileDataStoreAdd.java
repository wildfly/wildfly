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

import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.ejb3.timerservice.persistence.filestore.FileTimerPersistence;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleLoader;

/**
 * Adds the timer service file based data store
 *
 * @author Stuart Douglas
 */
public class FileDataStoreAdd extends AbstractAddStepHandler {

    private static final String TRANSACTION_SYNCHRONIZATION_REGISTRY_CAPABILITY_NAME = "org.wildfly.transactions.transaction-synchronization-registry";
    private static final String TRANSACTION_GLOBAL_DEFAULT_LOCAL_PROVIDER_CAPABILITY_NAME = "org.wildfly.transactions.global-default-local-provider";
    private static final String PATH_MANAGER_CAPABILITY_NAME = "org.wildfly.management.path-manager";

    FileDataStoreAdd(AttributeDefinition... attributes) {
        super(attributes);
    }

    protected void performRuntime(final OperationContext context, ModelNode operation, final ModelNode model) throws OperationFailedException {
        final ModelNode pathNode = FileDataStoreResourceDefinition.PATH.resolveModelAttribute(context, model);
        final String path = pathNode.isDefined() ? pathNode.asString() : null;
        final ModelNode relativeToNode = FileDataStoreResourceDefinition.RELATIVE_TO.resolveModelAttribute(context, model);
        final String relativeTo = relativeToNode.isDefined() ? relativeToNode.asString() : null;

        final FileTimerPersistence fileTimerPersistence = new FileTimerPersistence(true, path, relativeTo);

        // add the TimerPersistence instance
        final CapabilityServiceTarget serviceTarget = context.getCapabilityServiceTarget();
        final CapabilityServiceBuilder<FileTimerPersistence> builder = serviceTarget.addCapability(FileDataStoreResourceDefinition.TIMER_PERSISTENCE_CAPABILITY, fileTimerPersistence);
        builder.addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, fileTimerPersistence.getModuleLoader());
        builder.addCapabilityRequirement(PATH_MANAGER_CAPABILITY_NAME, PathManager.class, fileTimerPersistence.getPathManager());
        builder.addCapabilityRequirement(TRANSACTION_GLOBAL_DEFAULT_LOCAL_PROVIDER_CAPABILITY_NAME, Void.class);
        builder.addCapabilityRequirement(TRANSACTION_SYNCHRONIZATION_REGISTRY_CAPABILITY_NAME, TransactionSynchronizationRegistry.class, fileTimerPersistence.getTransactionSynchronizationRegistry());
        builder.install();
    }
}
