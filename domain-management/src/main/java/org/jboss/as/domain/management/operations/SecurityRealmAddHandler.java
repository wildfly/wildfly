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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_IDENTITIES;

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
import org.jboss.as.domain.management.connections.ConnectionManager;
import org.jboss.as.domain.management.security.LdapConnectionManagerService;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * Handler to add security realm definitions and register the service.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SecurityRealmAddHandler implements ModelAddOperationHandler, DescriptionProvider {

    public static final SecurityRealmAddHandler INSTANCE = new SecurityRealmAddHandler();
    public static final String OPERATION_NAME = ModelDescriptionConstants.ADD;

    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
        ModelNode operationAddress = operation.require(OP_ADDR);
        PathAddress address = PathAddress.pathAddress(operationAddress);
        final String securityRealm = address.getLastElement().getValue();
        final ModelNode authentication = operation.get(AUTHENTICATION);
        final ModelNode serverIdentities = operation.get(SERVER_IDENTITIES);

        final ModelNode subModel = context.getSubModel();
        if (serverIdentities != null) {
            subModel.get(SERVER_IDENTITIES).set(serverIdentities);
        }
        if (authentication != null) {
            subModel.get(AUTHENTICATION).set(authentication);
        }

        final ModelNode compensatingOperation = new ModelNode(); // TODO - Complete the remove.

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceTarget serviceTarget = context.getServiceTarget();

                    final SecurityRealmService securityRealmService = new SecurityRealmService(securityRealm, authentication, serverIdentities);

                    ServiceBuilder builder = serviceTarget.addService(SecurityRealmService.BASE_SERVICE_NAME.append(securityRealm), securityRealmService);

                    String connectionManager = requiredConnectionManager(authentication);
                    if (connectionManager != null) {
                        builder.addDependency(LdapConnectionManagerService.BASE_SERVICE_NAME.append(connectionManager), ConnectionManager.class, securityRealmService.getConnectionManagerInjector());
                    }

                    builder.setInitialMode(ServiceController.Mode.ON_DEMAND)
                            .install();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensatingOperation);
    }

    private static String requiredConnectionManager(ModelNode authentication) {
        String connectionManager = null;
        if (authentication.has(LDAP)) {
            ModelNode userLdap = authentication.require(LDAP);
            connectionManager = userLdap.require(CONNECTION).asString();
        }

        return connectionManager;
    }


    @Override
    public ModelNode getModelDescription(Locale locale) {
        // TODO - Complete getModelDescription()
        return new ModelNode();
    }
}
