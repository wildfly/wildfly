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
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.services.path.AbsolutePathService;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.omg.CORBA.ORB;

/**
 * Adds a recovery-environment to the Transactions subsystem's
 *
 */
public class CoreEnvironmentAdd extends AbstractBoottimeAddStepHandler implements DescriptionProvider {

    public static final CoreEnvironmentAdd INSTANCE = new CoreEnvironmentAdd();
    private static final ServiceName INTERNAL_CORE_ENV_VAR_PATH = TxnServices.JBOSS_TXN_PATHS.append("core-var-dir");

    public static final SimpleAttributeDefinition NODE_IDENTIFIER = new SimpleAttributeDefinitionBuilder(CommonAttributes.NODE_IDENTIFIER, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("1"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition PROCESS_ID_UUID = new SimpleAttributeDefinitionBuilder("process-id-uuid", ModelType.BOOLEAN, false)
            .setAlternatives("process-id-socket-binding")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition PROCESS_ID_SOCKET_BINDING = new SimpleAttributeDefinitionBuilder("process-id-socket-binding", ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1, true))
            .setAlternatives("process-id-uuid")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.BINDING.getLocalName())
            .build();

    public static final SimpleAttributeDefinition PROCESS_ID_SOCKET_MAX_PORTS = new SimpleAttributeDefinitionBuilder("process-id-socket-max-ports", ModelType.INT, true)
            .setValidator(new IntRangeValidator(1, true))
            .setDefaultValue(new ModelNode().set(10))
            .setRequires("process-id-socket-binding")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.SOCKET_PROCESS_ID_MAX_PORTS.getLocalName())
            .build();

    public static final SimpleAttributeDefinition RELATIVE_TO = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.RELATIVE_TO, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("jboss.server.data.dir"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition PATH = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PATH, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("var"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();


    /**
     * Description provider for the strict-max-pool add operation
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        // TODO use a ResourceDefinition and StandardResourceDescriptionResolver for this resource
        return Descriptions.getCoreEnvironmentAddDescription(locale);
    }


    @Override
    protected void populateModel(ModelNode operation, ModelNode coreEnvModel) throws OperationFailedException {

        NODE_IDENTIFIER.validateAndSet(operation, coreEnvModel);
        PATH.validateAndSet(operation, coreEnvModel);
        RELATIVE_TO.validateAndSet(operation, coreEnvModel);

        // We have some complex logic for the 'process-id' stuff because of the alternatives
        if (operation.hasDefined(PROCESS_ID_UUID.getName()) && operation.get(PROCESS_ID_UUID.getName()).asBoolean()) {
            PROCESS_ID_UUID.validateAndSet(operation, coreEnvModel);
            if (operation.hasDefined(PROCESS_ID_SOCKET_BINDING.getName())) {
                throw new OperationFailedException(new ModelNode().set(String.format("%s must be undefined if %s is 'true'.",
                        PROCESS_ID_SOCKET_BINDING.getName(), PROCESS_ID_UUID.getName())));
            } else if (operation.hasDefined(PROCESS_ID_SOCKET_MAX_PORTS.getName())) {
                throw new OperationFailedException(new ModelNode().set(String.format("%s must be undefined if %s is 'true'.",
                        PROCESS_ID_SOCKET_MAX_PORTS.getName(), PROCESS_ID_UUID.getName())));
            }
            coreEnvModel.get(PROCESS_ID_SOCKET_BINDING.getName());
            coreEnvModel.get(PROCESS_ID_SOCKET_MAX_PORTS.getName());
        } else if (operation.hasDefined(PROCESS_ID_SOCKET_BINDING.getName())) {
            PROCESS_ID_SOCKET_BINDING.validateAndSet(operation, coreEnvModel);
            PROCESS_ID_SOCKET_MAX_PORTS.validateAndSet(operation, coreEnvModel);
            coreEnvModel.get(PROCESS_ID_UUID.getName()).set(false);
        } else if (operation.hasDefined(PROCESS_ID_SOCKET_MAX_PORTS.getName())) {
            throw new OperationFailedException(new ModelNode().set(String.format("%s must be defined if %s is defined.",
                    PROCESS_ID_SOCKET_BINDING.getName(), PROCESS_ID_SOCKET_MAX_PORTS.getName())));
        } else {
            // not uuid and also not sockets!
            throw new OperationFailedException(new ModelNode().set(String.format("Either %s must be 'true' or  %s must be defined.",
                    PROCESS_ID_UUID.getName(), PROCESS_ID_SOCKET_BINDING.getName())));
        }
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode coreEnvModel,
                                  ServiceVerificationHandler verificationHandler,
                                  List<ServiceController<?>> controllers) throws OperationFailedException {

        // Configure the core configuration.
        final String nodeIdentifier = NODE_IDENTIFIER.validateResolvedOperation(coreEnvModel).asString();
        final CoreEnvironmentService coreEnvironmentService = new CoreEnvironmentService(nodeIdentifier);

        String socketBindingName = null;
        if (PROCESS_ID_UUID.validateResolvedOperation(coreEnvModel).asBoolean()) {
            // Use the UUID based id
            UuidProcessId id = new UuidProcessId();
            coreEnvironmentService.setProcessImplementation(id);
        } else {
            // Use the socket process id
            coreEnvironmentService.setProcessImplementationClassName(ProcessIdType.SOCKET.getClazz());
            socketBindingName = PROCESS_ID_SOCKET_BINDING.validateResolvedOperation(coreEnvModel).asString();
            int ports = PROCESS_ID_SOCKET_MAX_PORTS.validateResolvedOperation(coreEnvModel).asInt();
            coreEnvironmentService.setSocketProcessIdMaxPorts(ports);
        }

        String varDirPathRef = null;
        // Check for empty string value for relative-to, which disables the default
        final ModelNode relativePathNode = coreEnvModel.get(RELATIVE_TO.getName());
        if (!relativePathNode.isDefined() || relativePathNode.asString().length() > 0) {
            varDirPathRef = RELATIVE_TO.validateResolvedOperation(coreEnvModel).asString();
        }
        final String varDirPath = PATH.validateResolvedOperation(coreEnvModel).asString();

        if (ROOT_LOGGER.isDebugEnabled()) {
            ROOT_LOGGER.debugf("nodeIdentifier=%s\n", nodeIdentifier);
            ROOT_LOGGER.debugf("varDirPathRef=%s, varDirPath=%s\n", varDirPathRef, varDirPath);
        }

        ServiceTarget target = context.getServiceTarget();
        ServiceController<String> varDirRPS = varDirPathRef != null
                ? RelativePathService.addService(INTERNAL_CORE_ENV_VAR_PATH, varDirPath, varDirPathRef, target)
                : AbsolutePathService.addService(INTERNAL_CORE_ENV_VAR_PATH, varDirPath, target);
        controllers.add(varDirRPS);

        ServiceBuilder<?> coreEnvBuilder = context.getServiceTarget().addService(TxnServices.JBOSS_TXN_CORE_ENVIRONMENT, coreEnvironmentService);
        if (socketBindingName != null) {
            // Add a dependency on the socket id binding
            ServiceName bindingName = SocketBinding.JBOSS_BINDING_NAME.append(socketBindingName);
            coreEnvBuilder.addDependency(bindingName, SocketBinding.class, coreEnvironmentService.getSocketProcessBindingInjector());
        }
        controllers.add(coreEnvBuilder.addDependency(INTERNAL_CORE_ENV_VAR_PATH, String.class, coreEnvironmentService.getPathInjector())
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install());
    }

}
