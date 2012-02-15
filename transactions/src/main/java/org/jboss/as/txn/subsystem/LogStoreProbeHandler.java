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

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * Handler for transaction manager metrics
 *
 * @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a> (c) 2011 Red Hat Inc.
 */
public class LogStoreProbeHandler implements OperationStepHandler {

    static final LogStoreProbeHandler INSTANCE = new LogStoreProbeHandler();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        //TODO populating  from jmx

        final ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        final ManagementResourceRegistration registration = context.getResourceRegistrationForUpdate();
        model.get(LogStoreConstans.LOG_STORE_TYPE.getName()).set("xxx");

        final ModelNode transactionAddress = operation.get("address").clone();
        final ModelNode transactionOperation = new ModelNode();
        operation.get(OP).set(ADD);
        transactionAddress.add(LogStoreConstans.TRANSACTIONS, "1");

        transactionAddress.protect();

        transactionOperation.get(OP_ADDR).set(transactionAddress);
        transactionOperation.get(LogStoreConstans.TRANSACTION_ID.getName()).set(1);
        transactionOperation.get(LogStoreConstans.TRANSACTION_AGE.getName()).set(100);

        context.addStep(transactionOperation, LogStoreTransactionAddHandler.INSTANCE, OperationContext.Stage.MODEL);


        final ModelNode partecipantAddress = transactionAddress.clone();
        final ModelNode partecipantOperation = new ModelNode();
        operation.get(OP).set(ADD);

        final String jndiName = "java:/MyJndi";

        partecipantAddress.add(LogStoreConstans.PARTECIPANTS, jndiName);

        partecipantAddress.protect();

        partecipantOperation.get(OP_ADDR).set(partecipantAddress);
        partecipantOperation.get(LogStoreConstans.PARTECIPANT_STATUS.getName()).set(LogStoreConstans.PartecipatnStatus.Heuristic.name());
        partecipantOperation.get(LogStoreConstans.PARTECIPANT_JNDI_NAME.getName()).set(jndiName);

        context.addStep(partecipantOperation, LogStorePartecipantAddHandler.INSTANCE, OperationContext.Stage.MODEL);

        context.completeStep();


    }
}
