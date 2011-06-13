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

package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_MODEL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.NewModelController.OperationTransaction;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewProxyController;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.domain.controller.NewDomainController;
import org.jboss.as.domain.controller.UnregisteredHostChannelRegistry;
import org.jboss.dmr.ModelNode;

/**
 * Step handler responsible for pushing our master domain model to the remote slave
 * as part of the remote slave's registration with this master domain controller.
 *
 * @author John Bailey
 */
public class ReadMasterDomainModelHandler implements NewStepHandler, DescriptionProvider {
    public static final String OPERATION_NAME = "read-master-domain-model";

    private final NewDomainController domainController;
    private final UnregisteredHostChannelRegistry registry;

    public ReadMasterDomainModelHandler(final NewDomainController domainController, final UnregisteredHostChannelRegistry registry) {
        this.domainController = domainController;
        this.registry = registry;
    }


    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
        //Lock the model here so
        context.getServiceRegistry(true);
        final ModelNode model = context.getModel();
        final String hostName = operation.get(HOST).asString();

        ModelNode op = new ModelNode();
        op.get(OP).set(ApplyRemoteMasterDomainModelHandler.OPERATION_NAME);
        op.get(OP_ADDR).setEmptyList();
        op.get(DOMAIN_MODEL).set(context.getModel());

        //TODO get this from somewhere
        final NewProxyController proxy = registry.popChannelAndCreateProxy(hostName);

        final AtomicReference<ModelNode> failedRef = new AtomicReference<ModelNode>();
        final AtomicReference<ModelNode> preparedRef = new AtomicReference<ModelNode>();
        final AtomicReference<OperationTransaction> txRef = new AtomicReference<OperationTransaction>();
        NewProxyController.ProxyOperationControl control = new NewProxyController.ProxyOperationControl() {

            @Override
            public void operationFailed(ModelNode response) {
                failedRef.set(response);
            }

            @Override
            public void operationPrepared(OperationTransaction transaction, ModelNode result) {
                txRef.set(transaction);
                preparedRef.set(result);
            }

            @Override
            public void operationCompleted(ModelNode response) {
            }
        };
        proxy.execute(op, OperationMessageHandler.logging, control, null);

        if (failedRef.get() != null) {
            final ModelNode failed = failedRef.get();
            context.getResult().set(failed.get(RESULT));
            context.getFailureDescription().set(failed.get(FAILURE_DESCRIPTION));
            context.completeStep();
        } else {
            final ModelNode preparedResult = preparedRef.get();
            context.getResult().set(preparedResult.get(RESULT));
            if (preparedResult.hasDefined(FAILURE_DESCRIPTION)) {
                context.getFailureDescription().set(preparedResult.get(FAILURE_DESCRIPTION));
            }

            NewOperationContext.ResultAction resultAction = context.completeStep();
            NewModelController.OperationTransaction tx = txRef.get();
            if (tx != null) {
                if (resultAction == NewOperationContext.ResultAction.KEEP) {
                    tx.commit();
                    domainController.registerRemoteHost(proxy);
                } else {
                    tx.rollback();
                }
            }
        }
    }

    public ModelNode getModelDescription(Locale locale) {
        return new ModelNode(); // PRIVATE operation requires no description
    }
}
