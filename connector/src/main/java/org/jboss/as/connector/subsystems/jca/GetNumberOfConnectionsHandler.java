/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
