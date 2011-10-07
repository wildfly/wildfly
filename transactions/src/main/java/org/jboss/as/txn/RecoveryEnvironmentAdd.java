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
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.omg.CORBA.ORB;

/**
 * Adds a recovery-environment to the Transactions subsystem
 *
 */
public class RecoveryEnvironmentAdd extends AbstractBoottimeAddStepHandler implements DescriptionProvider {

    public static final RecoveryEnvironmentAdd INSTANCE = new RecoveryEnvironmentAdd();

    public static final SimpleAttributeDefinition BINDING = new SimpleAttributeDefinitionBuilder(CommonAttributes.BINDING, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.BINDING.getLocalName())
            .build();

    public static final SimpleAttributeDefinition STATUS_BINDING = new SimpleAttributeDefinitionBuilder(CommonAttributes.STATUS_BINDING, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.STATUS_BINDING.getLocalName())
            .build();

    public static final SimpleAttributeDefinition RECOVERY_LISTENER = new SimpleAttributeDefinitionBuilder(CommonAttributes.RECOVERY_LISTENER, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.RECOVERY_LISTENER.getLocalName())
            .build();

    /**
     * Description provider for the add operation
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        // TODO use a ResourceDefinition and StandardResourceDescriptionResolver for this resource
        return Descriptions.getRecoveryEnvironmentAddDescription(locale);
    }


    @Override
    protected void populateModel(ModelNode operation, ModelNode recoveryEnvModel) throws OperationFailedException {

        BINDING.validateAndSet(operation, recoveryEnvModel);
        STATUS_BINDING.validateAndSet(operation, recoveryEnvModel);
        RECOVERY_LISTENER.validateAndSet(operation, recoveryEnvModel);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode recoveryEnvModel,
                                  ServiceVerificationHandler verificationHandler,
                                  List<ServiceController<?>> serviceControllers) throws OperationFailedException {

        final String recoveryBindingName = BINDING.validateResolvedOperation(recoveryEnvModel).asString();
        final String recoveryStatusBindingName = STATUS_BINDING.validateResolvedOperation(recoveryEnvModel).asString();
        final boolean recoveryListener = RECOVERY_LISTENER.validateResolvedOperation(recoveryEnvModel).asBoolean();

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
