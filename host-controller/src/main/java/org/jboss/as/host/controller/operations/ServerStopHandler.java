/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.host.controller.operations;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Locale;

import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.host.controller.HostController;
import org.jboss.as.host.controller.descriptions.HostRootDescription;
import org.jboss.dmr.ModelNode;

/**
 * Stops a server.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ServerStopHandler implements NewStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "stop";

    public static ModelNode getStopServerOperation(String serverName) {
        ModelNode op = Util.getEmptyOperation(OPERATION_NAME, new ModelNode());
        op.get(NAME).set(serverName);

        return op;
    }

    private final HostController hostController;

    /**
     * Create the ServerAddHandler
     */
    public ServerStopHandler(final HostController hostController) {
        this.hostController = hostController;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final PathElement element = address.getLastElement();
        final String serverName = element.getValue();

        context.addStep(new NewStepHandler() {
            @Override
            public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
                final ServerStatus status = hostController.stopServer(serverName);
                context.getResult().set(status.toString());
                context.completeStep();
            }
        }, NewOperationContext.Stage.RUNTIME);
        context.completeStep();
    }

    @Override
    public ModelNode getModelDescription(final Locale locale) {
        return HostRootDescription.getStopServerOperation(locale);
    }
}
