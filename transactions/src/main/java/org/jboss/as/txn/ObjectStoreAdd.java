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

package org.jboss.as.txn;

import static org.jboss.as.txn.TransactionLogger.ROOT_LOGGER;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.server.services.path.AbsolutePathService;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Adds a recovery-environment to the Transactions subsystem
 *
 */
public class ObjectStoreAdd extends AbstractBoottimeAddStepHandler implements DescriptionProvider {

    public static final ObjectStoreAdd INSTANCE = new ObjectStoreAdd();
    static final ServiceName INTERNAL_OBJECTSTORE_PATH = TxnServices.JBOSS_TXN_PATHS.append("object-store");

    public static final SimpleAttributeDefinition RELATIVE_TO = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.RELATIVE_TO, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("jboss.server.data.dir"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.RELATIVE_TO.getLocalName())
            .build();

    public static final SimpleAttributeDefinition PATH = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PATH, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("tx-object-store"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.PATH.getLocalName())
            .build();


    /**
     * Description provider for the strict-max-pool add operation
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        // TODO use a ResourceDefinition and StandardResourceDescriptionResolver for this resource
        return Descriptions.getObjectStoreAddDescription(locale);
    }


    @Override
    protected void populateModel(ModelNode operation, ModelNode objectStoreModel) throws OperationFailedException {

        RELATIVE_TO.validateAndSet(operation, objectStoreModel);
        PATH.validateAndSet(operation, objectStoreModel);

    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode recoveryEnvModel,
                                  ServiceVerificationHandler verificationHandler,
                                  List<ServiceController<?>> controllers) throws OperationFailedException {

        String objectStorePathRef = null;
        // Check for empty string value for relative-to, which disables the default
        final ModelNode relativePathNode = recoveryEnvModel.get(RELATIVE_TO.getName());
        if (!relativePathNode.isDefined() || relativePathNode.asString().length() > 0) {
            objectStorePathRef = RELATIVE_TO.validateResolvedOperation(recoveryEnvModel).asString();
        }
        final String objectStorePath = PATH.validateResolvedOperation(recoveryEnvModel).asString();
        if (ROOT_LOGGER.isDebugEnabled()) {
            ROOT_LOGGER.debugf("objectStorePathRef=%s, objectStorePath=%s\n", objectStorePathRef, objectStorePath);
        }

        ServiceTarget target = context.getServiceTarget();
        // Configure the ObjectStoreEnvironmentBeans
        ServiceController<String> objectStorePS = objectStorePathRef != null
                ? RelativePathService.addService(INTERNAL_OBJECTSTORE_PATH, objectStorePath, objectStorePathRef, target)
                : AbsolutePathService.addService(INTERNAL_OBJECTSTORE_PATH, objectStorePath, target);
        controllers.add(objectStorePS);

        final boolean useHornetqJournalStore = "true".equals(System.getProperty("usehornetqstore")); // TODO wire to domain model instead.

        final ArjunaObjectStoreEnvironmentService objStoreEnvironmentService = new ArjunaObjectStoreEnvironmentService(useHornetqJournalStore);
        controllers.add(target.addService(TxnServices.JBOSS_TXN_ARJUNA_OBJECTSTORE_ENVIRONMENT, objStoreEnvironmentService)
                .addDependency(INTERNAL_OBJECTSTORE_PATH, String.class, objStoreEnvironmentService.getPathInjector())
                .addDependency(TxnServices.JBOSS_TXN_CORE_ENVIRONMENT)
                .addListener(verificationHandler).setInitialMode(ServiceController.Mode.ACTIVE).install());

        controllers.add(TransactionManagerService.addService(target, verificationHandler));
        controllers.add(UserTransactionService.addService(target, verificationHandler));
        controllers.add(target.addService(TxnServices.JBOSS_TXN_USER_TRANSACTION_REGISTRY, new UserTransactionRegistryService())
                .addListener(verificationHandler).setInitialMode(ServiceController.Mode.ACTIVE).install());
        controllers.add(TransactionSynchronizationRegistryService.addService(target, verificationHandler));

    }

}
