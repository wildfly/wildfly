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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.txn.CommonAttributes.BINDING;
import static org.jboss.as.txn.CommonAttributes.NODE_IDENTIFIER;
import static org.jboss.as.txn.CommonAttributes.OBJECT_STORE;
import static org.jboss.as.txn.CommonAttributes.PROCESS_ID;
import static org.jboss.as.txn.CommonAttributes.SOCKET_PROCESS_ID_MAX_PORTS;
import static org.jboss.as.txn.TransactionLogger.ROOT_LOGGER;

import java.util.List;
import java.util.Locale;

import com.arjuna.ats.internal.arjuna.utils.UuidProcessId;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Adds a recovery-environment to the Transactions subsystem's
 *
 */
public class ObjectStoreAdd extends AbstractAddStepHandler implements DescriptionProvider {

    public static final ObjectStoreAdd INSTANCE = new ObjectStoreAdd();
    private static final ServiceName INTERNAL_OBJECTSTORE_PATH = TxnServices.JBOSS_TXN_PATHS.append("object-store");



    /**
     * Description provider for the strict-max-pool add operation
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return Descriptions.getRecoveryEnvironmentAddDescription(locale);
    }


    @Override
    protected void populateModel(ModelNode operation, ModelNode objectStoreModel) throws OperationFailedException {

        objectStoreModel.get(RELATIVE_TO).set(operation.get(RELATIVE_TO));
        objectStoreModel.get(PATH).set(operation.get(PATH));

    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode recoveryEnvModel,
                                  ServiceVerificationHandler verificationHandler,
                                  List<ServiceController<?>> controllers) throws OperationFailedException {
        final ModelNode objectStore = operation.get(OBJECT_STORE);
        final String objectStorePathRef = objectStore.hasDefined(RELATIVE_TO) ? objectStore.get(RELATIVE_TO).asString() : "jboss.server.data.dir";
        final String objectStorePath = objectStore.hasDefined(PATH) ? objectStore.get(PATH).asString() : "tx-object-store";
        if (ROOT_LOGGER.isDebugEnabled()) {
            ROOT_LOGGER.debugf("objectStorePathRef=%s, objectStorePathRef=%s\n", objectStorePathRef, objectStorePath);
        }

        ServiceTarget target = context.getServiceTarget();
        // Configure the ObjectStoreEnvironmentBeans
        ServiceController<String> objectStoreRPS = RelativePathService.addService(INTERNAL_OBJECTSTORE_PATH, objectStorePath, objectStorePathRef, target);
        controllers.add(objectStoreRPS);

        final boolean useHornetqJournalStore = "true".equals(System.getProperty("usehornetqstore")); // TODO wire to domain model instead.

        final ArjunaObjectStoreEnvironmentService objStoreEnvironmentService = new ArjunaObjectStoreEnvironmentService(useHornetqJournalStore);
        controllers.add(target.addService(TxnServices.JBOSS_TXN_ARJUNA_OBJECTSTORE_ENVIRONMENT, objStoreEnvironmentService)
                .addDependency(objectStoreRPS.getName(), String.class, objStoreEnvironmentService.getPathInjector())
                .addDependency(TxnServices.JBOSS_TXN_CORE_ENVIRONMENT)
                .addListener(verificationHandler).setInitialMode(ServiceController.Mode.ACTIVE).install());

        controllers.add(TransactionManagerService.addService(target, verificationHandler));
        controllers.add(UserTransactionService.addService(target, verificationHandler));
        controllers.add(target.addService(TxnServices.JBOSS_TXN_USER_TRANSACTION_REGISTRY, new UserTransactionRegistryService())
                .addListener(verificationHandler).setInitialMode(ServiceController.Mode.ACTIVE).install());
        controllers.add(TransactionSynchronizationRegistryService.addService(target, verificationHandler));

    }

}
