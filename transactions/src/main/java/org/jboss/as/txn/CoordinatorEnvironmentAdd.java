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

import static org.jboss.as.txn.CommonAttributes.COORDINATOR_ENVIRONMENT;
import static org.jboss.as.txn.CommonAttributes.DEFAULT_TIMEOUT;
import static org.jboss.as.txn.CommonAttributes.ENABLE_STATISTICS;
import static org.jboss.as.txn.CommonAttributes.ENABLE_TSM_STATUS;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.tm.JBossXATerminator;
import org.omg.CORBA.ORB;

/**
 * Adds a recovery-environment to the Transactions subsystem's
 */
public class CoordinatorEnvironmentAdd extends AbstractBoottimeAddStepHandler implements DescriptionProvider {

    public static final CoordinatorEnvironmentAdd INSTANCE = new CoordinatorEnvironmentAdd();


    /**
     * Description provider for the strict-max-pool add operation
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return Descriptions.getRecoveryEnvironmentAddDescription(locale);
    }


    @Override
    protected void populateModel(ModelNode operation, ModelNode coreEnvModel) throws OperationFailedException {
        final boolean coordinatorEnableStatistics = operation.get(ENABLE_STATISTICS).asBoolean(true);
        final boolean transactionStatusManagerEnable = operation.get(ENABLE_TSM_STATUS).asBoolean(false);
        final int coordinatorDefaultTimeout = operation.get(DEFAULT_TIMEOUT).asInt(300);


        coreEnvModel.get(ENABLE_STATISTICS).set(coordinatorEnableStatistics);
        coreEnvModel.get(ENABLE_TSM_STATUS).set(transactionStatusManagerEnable);
        coreEnvModel.get(DEFAULT_TIMEOUT).set(coordinatorDefaultTimeout);  // store the default so we write it -- TODO store all the defaults


    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode recoveryEnvModel,
                                  ServiceVerificationHandler verificationHandler,
                                  List<ServiceController<?>> controllers) throws OperationFailedException {

        final boolean coordinatorEnableStatistics = operation.get(ENABLE_STATISTICS).asBoolean(true);
        final boolean transactionStatusManagerEnable = operation.get(ENABLE_TSM_STATUS).asBoolean(false);
        final int coordinatorDefaultTimeout = operation.get(DEFAULT_TIMEOUT).asInt(300);

        final ArjunaTransactionManagerService transactionManagerService = new ArjunaTransactionManagerService(coordinatorEnableStatistics, coordinatorDefaultTimeout, transactionStatusManagerEnable);
        controllers.add(context.getServiceTarget().addService(TxnServices.JBOSS_TXN_ARJUNA_TRANSACTION_MANAGER, transactionManagerService)
                .addDependency(ServiceBuilder.DependencyType.OPTIONAL, ServiceName.JBOSS.append("iiop", "orb"), ORB.class, transactionManagerService.getOrbInjector())
                .addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class, transactionManagerService.getXaTerminatorInjector())
                .addDependency(TxnServices.JBOSS_TXN_CORE_ENVIRONMENT)
                .addDependency(TxnServices.JBOSS_TXN_ARJUNA_OBJECTSTORE_ENVIRONMENT)
                .addDependency(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER)
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install());


    }

}
