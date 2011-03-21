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

package org.jboss.as.domain.management.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INITIAL_CONTEXT_FACTORY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SEARCH_CREDENTIAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SEARCH_DN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;

import java.util.Locale;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.domain.management.security.LdapConnectionManagerService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * Handler for adding management connections.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ConnectionAddHandler implements ModelAddOperationHandler, DescriptionProvider {

    public static final ConnectionAddHandler INSTANCE = new ConnectionAddHandler();
    public static final String OPERATION_NAME = ModelDescriptionConstants.ADD;

    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
        ModelNode operationAddress = operation.require(OP_ADDR);
        PathAddress address = PathAddress.pathAddress(operationAddress);
        final String name = address.getLastElement().getValue();
        final String type = operation.get(TYPE).asString();

        final ModelNode subModel = context.getSubModel();
        subModel.get(TYPE).set(operation.require(TYPE).asString());
        subModel.get(URL).set(operation.require(URL).asString());
        subModel.get(SEARCH_DN).set(operation.require(SEARCH_DN).asString());
        subModel.get(SEARCH_CREDENTIAL).set(operation.require(SEARCH_CREDENTIAL).asString());
        if (operation.has(INITIAL_CONTEXT_FACTORY)) {
            subModel.get(INITIAL_CONTEXT_FACTORY).set(operation.require(INITIAL_CONTEXT_FACTORY).asString());
        }

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceTarget serviceTarget = context.getServiceTarget();
                    if (LDAP.equals(type)) {
                        final LdapConnectionManagerService connectionManagerService = new LdapConnectionManagerService(subModel);

                        serviceTarget.addService(LdapConnectionManagerService.BASE_SERVICE_NAME.append(name), connectionManagerService)
                                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                                .install();
                    }
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
        final ModelNode compensatingOperation = new ModelNode(); // TODO - Complete the remove.
        return new BasicOperationResult(compensatingOperation);
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        // TODO - Complete getModelDescription()
        return new ModelNode();
    }

}
