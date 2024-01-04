/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 *
 */
package org.jboss.as.connector.subsystems.jca;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;

/**
 *
 * @author Stefano Maestri (c) 2011 Red Hat Inc.
 */
public class GetNumberOfConnectionsHandler implements OperationStepHandler {

    public static final GetNumberOfConnectionsHandler INSTANCE = new GetNumberOfConnectionsHandler();

    private GetNumberOfConnectionsHandler() {

    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        if (context.isNormalServer()) {
            context.addStep(new OperationStepHandler() {

                @Override
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                    ModelNode result = new ModelNode();

                    CachedConnectionManager ccm = (CachedConnectionManager) context.getServiceRegistry(false).getService(ConnectorServices.CCM_SERVICE).getValue();
                    ModelNode txResult = new ModelNode().set(ccm.getNumberOfConnections());

                    ccm = (CachedConnectionManager) context.getServiceRegistry(false).getService(ConnectorServices.NON_TX_CCM_SERVICE).getValue();
                    ModelNode nonTxResult = new ModelNode().set(ccm.getNumberOfConnections());

                    result.get(Constants.TX).set(txResult);
                    result.get(Constants.NON_TX).set(nonTxResult);

                    context.getResult().set(result);
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }
}
