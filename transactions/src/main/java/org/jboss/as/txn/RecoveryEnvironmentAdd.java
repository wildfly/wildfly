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

import static org.jboss.as.txn.CommonAttributes.BINDING;
import static org.jboss.as.txn.CommonAttributes.RECOVERY_LISTENER;
import static org.jboss.as.txn.CommonAttributes.STATUS_BINDING;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.omg.CORBA.ORB;

/**
 * Adds a recovery-environment to the Transactions subsystem's
 *
 */
public class RecoveryEnvironmentAdd extends AbstractAddStepHandler implements DescriptionProvider {

    public static final RecoveryEnvironmentAdd INSTANCE = new RecoveryEnvironmentAdd();

    /**
     * Description provider for the strict-max-pool add operation
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return Descriptions.getRecoveryEnvironmentAddDescription(locale);
    }


    @Override
    protected void populateModel(ModelNode operation, ModelNode recoveryEnvModel) throws OperationFailedException {
        final String recoveryBindingName = operation.require(BINDING).asString();
        final String recoveryStatusBindingName = operation.require(STATUS_BINDING).asString();
        final boolean recoveryListener = operation.get(RECOVERY_LISTENER).asBoolean(false);

        recoveryEnvModel.get(BINDING).set(recoveryBindingName);
        recoveryEnvModel.get(STATUS_BINDING).set(recoveryStatusBindingName);
        recoveryEnvModel.get(RECOVERY_LISTENER).set(operation.get(RECOVERY_LISTENER));

    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode recoveryEnvModel,
                                  ServiceVerificationHandler verificationHandler,
                                  List<ServiceController<?>> serviceControllers) throws OperationFailedException {

        final String recoveryBindingName = recoveryEnvModel.require(BINDING).asString();
        final String recoveryStatusBindingName = recoveryEnvModel.require(STATUS_BINDING).asString();
        final boolean recoveryListener = recoveryEnvModel.get(RECOVERY_LISTENER).asBoolean(false);

        final ArjunaRecoveryManagerService recoveryManagerService = new ArjunaRecoveryManagerService(recoveryListener);
                                   serviceControllers.add(context.getServiceTarget().addService(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER, recoveryManagerService)
                                           .addDependency(ServiceBuilder.DependencyType.OPTIONAL, ServiceName.JBOSS.append("iiop", "orb"), ORB.class, recoveryManagerService.getOrbInjector())
                                           .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(recoveryBindingName), SocketBinding.class, recoveryManagerService.getRecoveryBindingInjector())
                                           .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(recoveryStatusBindingName), SocketBinding.class, recoveryManagerService.getStatusBindingInjector())
                                           .addDependency(TxnServices.JBOSS_TXN_CORE_ENVIRONMENT)
                                           .addDependency(TxnServices.JBOSS_TXN_ARJUNA_OBJECTSTORE_ENVIRONMENT)
                                           .addListener(verificationHandler)
                                           .setInitialMode(ServiceController.Mode.ACTIVE)
                                           .install());

    }

}
