/*
 * Copyright 2019 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.messaging.activemq.ha;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;

import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class HAPolicySynchronizationStatusReadHandler extends AbstractRuntimeOnlyHandler {

    public static final AttributeDefinition SYNCHRONIZED_WITH_LIVE
            = new SimpleAttributeDefinitionBuilder("synchronized-with-live", ModelType.BOOLEAN, false)
                    .setStorageRuntime()
                    .build();
    public static final AttributeDefinition SYNCHRONIZED_WITH_BACKUP
            = new SimpleAttributeDefinitionBuilder("synchronized-with-backup", ModelType.BOOLEAN, false)
                    .setStorageRuntime()
                    .build();

    public static final HAPolicySynchronizationStatusReadHandler INSTANCE = new HAPolicySynchronizationStatusReadHandler();

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        final String operationName = operation.require(OP).asString();
        try {
            if (READ_ATTRIBUTE_OPERATION.equals(operationName)) {
                ActiveMQServerControl serverControl = getServerControl(context, operation);
                context.getResult().set(serverControl.isReplicaSync());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            context.getFailureDescription().set(e.getLocalizedMessage());
        }
    }

    public static void registerSlaveAttributes(final ManagementResourceRegistration registry) {
        registry.registerReadOnlyAttribute(SYNCHRONIZED_WITH_LIVE, INSTANCE);
    }

    public static void registerMasterAttributes(final ManagementResourceRegistration registry) {
        registry.registerReadOnlyAttribute(SYNCHRONIZED_WITH_BACKUP, INSTANCE);
    }

    private ActiveMQServerControl getServerControl(final OperationContext context, ModelNode operation) throws OperationFailedException {
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> service = context.getServiceRegistry(false).getService(serviceName);
        if (service == null || service.getState() != ServiceController.State.UP) {
            throw MessagingLogger.ROOT_LOGGER.activeMQServerNotInstalled(serviceName.getSimpleName());
        }
        ActiveMQServer server = ActiveMQServer.class.cast(service.getValue());
        return server.getActiveMQServerControl();
    }
}
