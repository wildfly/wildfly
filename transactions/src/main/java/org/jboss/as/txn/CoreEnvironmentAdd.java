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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.txn.CommonAttributes.BINDING;
import static org.jboss.as.txn.CommonAttributes.CORE_ENVIRONMENT;
import static org.jboss.as.txn.CommonAttributes.NODE_IDENTIFIER;
import static org.jboss.as.txn.CommonAttributes.PROCESS_ID;
import static org.jboss.as.txn.CommonAttributes.RECOVERY_LISTENER;
import static org.jboss.as.txn.CommonAttributes.SOCKET_PROCESS_ID_MAX_PORTS;
import static org.jboss.as.txn.CommonAttributes.STATUS_BINDING;
import static org.jboss.as.txn.TransactionLogger.ROOT_LOGGER;

import java.util.List;
import java.util.Locale;

import com.arjuna.ats.internal.arjuna.utils.UuidProcessId;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.omg.CORBA.ORB;

/**
 * Adds a recovery-environment to the Transactions subsystem's
 *
 */
public class CoreEnvironmentAdd extends AbstractAddStepHandler implements DescriptionProvider {

    public static final CoreEnvironmentAdd INSTANCE = new CoreEnvironmentAdd();
    private static final ServiceName INTERNAL_CORE_ENV_VAR_PATH = TxnServices.JBOSS_TXN_PATHS.append("core-var-dir");


    /**
     * Description provider for the strict-max-pool add operation
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return Descriptions.getRecoveryEnvironmentAddDescription(locale);
    }


    @Override
    protected void populateModel(ModelNode operation, ModelNode coreEnvModel) throws OperationFailedException {
        final String nodeIdentifier = operation.hasDefined(NODE_IDENTIFIER) ? operation.get(NODE_IDENTIFIER).asString() : "1";
        final ModelNode processId = operation.require(PROCESS_ID);

        coreEnvModel.get(PROCESS_ID).set(operation.get(PROCESS_ID));
        coreEnvModel.get(NODE_IDENTIFIER).set(nodeIdentifier);


    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode recoveryEnvModel,
                                  ServiceVerificationHandler verificationHandler,
                                  List<ServiceController<?>> controllers) throws OperationFailedException {

        final String nodeIdentifier = operation.hasDefined(NODE_IDENTIFIER) ? operation.get(NODE_IDENTIFIER).asString() : "1";
        final ModelNode processId = operation.require(PROCESS_ID);
        final String varDirPathRef = operation.hasDefined(RELATIVE_TO) ? operation.get(RELATIVE_TO).asString() : "jboss.server.data.dir";
        final String varDirPath = operation.hasDefined(PATH) ? operation.get(PATH).asString() : "var";
        final int maxPorts = 10;

        if (ROOT_LOGGER.isDebugEnabled()) {
            ROOT_LOGGER.debugf("nodeIdentifier=%s\n", nodeIdentifier);
            ROOT_LOGGER.debugf("varDirPathRef=%s, varDirPath=%s\n", varDirPathRef, varDirPath);

        }

        // Configure the core configuration.
        String socketBindingName = null;
        final CoreEnvironmentService coreEnvironmentService = new CoreEnvironmentService(nodeIdentifier, varDirPath);
        if (processId.hasDefined(ProcessIdType.UUID.getName())) {
            // Use the UUID based id
            UuidProcessId id = new UuidProcessId();
            coreEnvironmentService.setProcessImplementation(id);
        } else if (processId.hasDefined(ProcessIdType.SOCKET.getName())) {
            // Use the socket process id
            coreEnvironmentService.setProcessImplementationClassName(ProcessIdType.SOCKET.getClazz());
            ModelNode socket = processId.get(ProcessIdType.SOCKET.getName());
            socketBindingName = socket.require(BINDING).asString();
            if (socket.hasDefined(SOCKET_PROCESS_ID_MAX_PORTS)) {
                int ports = socket.get(SOCKET_PROCESS_ID_MAX_PORTS).asInt(maxPorts);
                coreEnvironmentService.setSocketProcessIdMaxPorts(ports);
            }
        } else {
            // Default to UUID implementation
            UuidProcessId id = new UuidProcessId();
            coreEnvironmentService.setProcessImplementation(id);
        }
        ServiceBuilder<?> coreEnvBuilder = context.getServiceTarget().addService(TxnServices.JBOSS_TXN_CORE_ENVIRONMENT, coreEnvironmentService);
        if (socketBindingName != null) {
            // Add a dependency on the socket id binding
            ServiceName bindingName = SocketBinding.JBOSS_BINDING_NAME.append(socketBindingName);
            coreEnvBuilder.addDependency(bindingName, SocketBinding.class, coreEnvironmentService.getSocketProcessBindingInjector());
        }
        ServiceController<String> varDirRPS = RelativePathService.addService(INTERNAL_CORE_ENV_VAR_PATH, varDirPath, varDirPathRef, context.getServiceTarget());
        controllers.add(varDirRPS);
        controllers.add(coreEnvBuilder.addDependency(varDirRPS.getName(), String.class, coreEnvironmentService.getPathInjector())
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install());
    }

}
