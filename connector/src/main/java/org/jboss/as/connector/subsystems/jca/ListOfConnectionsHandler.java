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

import java.util.Map;

/**
 *
 * @author Stefano Maestri (c) 2011 Red Hat Inc.
 */
public class ListOfConnectionsHandler implements OperationStepHandler {

    public static final ListOfConnectionsHandler INSTANCE = new ListOfConnectionsHandler();

    private ListOfConnectionsHandler() {

    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        if (context.isNormalServer()) {
            context.addStep(new OperationStepHandler() {

                @Override
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                    ModelNode result = new ModelNode();

                    CachedConnectionManager ccm = (CachedConnectionManager) context.getServiceRegistry(false).getService(ConnectorServices.CCM_SERVICE).getValue();
                    Map<String, String> map = ccm.listConnections();
                    ModelNode txResult = new ModelNode();
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        txResult.add(entry.getKey(), entry.getValue());
                    }

                    ccm = (CachedConnectionManager) context.getServiceRegistry(false).getService(ConnectorServices.NON_TX_CCM_SERVICE).getValue();
                    map= ccm.listConnections();
                    ModelNode nonTxResult = new ModelNode();
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        nonTxResult.add(entry.getKey(), entry.getValue());
                    }

                    result.get(Constants.TX).set(txResult);
                    result.get(Constants.NON_TX).set(nonTxResult);
                    context.getResult().set(result);
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }
}
