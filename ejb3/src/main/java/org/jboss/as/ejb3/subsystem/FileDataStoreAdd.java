/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.controller.AbstractAddStepHandler;
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

    @Override
    protected void performRuntime(final OperationContext context, ModelNode operation, final ModelNode model) throws OperationFailedException {
        final ModelNode pathNode = FileDataStoreResourceDefinition.PATH.resolveModelAttribute(context, model);
        final String path = pathNode.isDefined() ? pathNode.asString() : null;
        final ModelNode relativeToNode = FileDataStoreResourceDefinition.RELATIVE_TO.resolveModelAttribute(context, model);
        final String relativeTo = relativeToNode.isDefined() ? relativeToNode.asString() : null;

        // add the TimerPersistence instance
        final CapabilityServiceTarget serviceTarget = context.getCapabilityServiceTarget();
        final CapabilityServiceBuilder<?> builder = serviceTarget.addCapability(TimerPersistenceResourceDefinition.CAPABILITY);
        final Consumer<FileTimerPersistence> consumer = builder.provides(TimerPersistenceResourceDefinition.CAPABILITY);
        builder.requiresCapability(TRANSACTION_GLOBAL_DEFAULT_LOCAL_PROVIDER_CAPABILITY_NAME, Void.class);
        final Supplier<TransactionSynchronizationRegistry> txnRegistrySupplier = builder.requiresCapability(TRANSACTION_SYNCHRONIZATION_REGISTRY_CAPABILITY_NAME, TransactionSynchronizationRegistry.class);
        final Supplier<ModuleLoader> moduleLoaderSupplier = builder.requires(Services.JBOSS_SERVICE_MODULE_LOADER);
        final Supplier<PathManager> pathManagerSupplier = builder.requires(PathManager.SERVICE_DESCRIPTOR);
        final FileTimerPersistence fileTimerPersistence = new FileTimerPersistence(consumer, txnRegistrySupplier, moduleLoaderSupplier, pathManagerSupplier, true, path, relativeTo);
        builder.setInstance(fileTimerPersistence);
        builder.install();
    }
}
