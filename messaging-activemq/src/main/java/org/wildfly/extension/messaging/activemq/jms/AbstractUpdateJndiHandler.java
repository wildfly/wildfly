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

package org.wildfly.extension.messaging.activemq.jms;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.messaging.activemq.ActiveMQActivationService.rollbackOperationIfServerNotActive;
import static org.wildfly.extension.messaging.activemq.logging.MessagingLogger.ROOT_LOGGER;

import org.apache.activemq.artemis.jms.server.JMSServerManager;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * Base class for handlers that handle "add-jndi" and "remove-jndi" operations.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class AbstractUpdateJndiHandler implements OperationStepHandler {

    private static final String ADD_JNDI = "add-jndi";
    private static final String REMOVE_JNDI = "remove-jndi";

    private static final SimpleAttributeDefinition JNDI_BINDING = new SimpleAttributeDefinitionBuilder(CommonAttributes.JNDI_BINDING, ModelType.STRING)
            .setRequired(true)
            .setValidator(new StringLengthValidator(1))
            .build();

    /**
     * {@code true} if the handler is for the add operation, {@code false} if it is for the remove operation.
     */
    private final boolean addOperation;

    protected void registerOperation(ManagementResourceRegistration registry, ResourceDescriptionResolver resolver) {
        SimpleOperationDefinition operation = new SimpleOperationDefinition(addOperation ? ADD_JNDI: REMOVE_JNDI,
                resolver,
                JNDI_BINDING);
        registry.registerOperationHandler(operation, this);
    }

    protected AbstractUpdateJndiHandler(boolean addOperation) {
        this.addOperation = addOperation;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        JNDI_BINDING.validateOperation(operation);
        final String jndiName = JNDI_BINDING.resolveModelAttribute(context, operation).asString();
        final ModelNode entries = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel().get(CommonAttributes.DESTINATION_ENTRIES.getName());

        if (addOperation) {
            for (ModelNode entry : entries.asList()) {
                if (jndiName.equals(entry.asString())) {
                    throw new OperationFailedException(ROOT_LOGGER.jndiNameAlreadyRegistered(jndiName));
                }
            }
            entries.add(jndiName);
        } else {
            ModelNode updatedEntries = new ModelNode();
            boolean updated = false;
            for (ModelNode entry : entries.asList()) {
                if (jndiName.equals(entry.asString())) {
                    if (entries.asList().size() == 1) {
                        throw new OperationFailedException(
                                ROOT_LOGGER.canNotRemoveLastJNDIName(jndiName));
                    }
                    updated = true;
                } else {
                    updatedEntries.add(entry);
                }
            }

            if (!updated) {
                throw MessagingLogger.ROOT_LOGGER.canNotRemoveUnknownEntry(jndiName);
            }

            context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel().get(CommonAttributes.DESTINATION_ENTRIES.getName()).set(updatedEntries);
        }


        if (context.isNormalServer()) {
            if (rollbackOperationIfServerNotActive(context, operation)) {
                return;
            }

            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final String resourceName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();

                    final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
                    final ServiceName jmsManagerServiceName = JMSServices.getJmsManagerBaseServiceName(serviceName);

                    final ServiceController<?> jmsServerService = context.getServiceRegistry(false).getService(jmsManagerServiceName);
                    if (jmsServerService != null) {
                        JMSServerManager jmsServerManager = JMSServerManager.class.cast(jmsServerService.getValue());

                        if (jmsServerManager == null) {
                            PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
                            throw ControllerLogger.ROOT_LOGGER.managementResourceNotFound(address);
                        }

                        try {
                            if (addOperation) {
                                addJndiName(jmsServerManager, resourceName, jndiName);
                            } else {
                                removeJndiName(jmsServerManager, resourceName, jndiName);
                            }
                        } catch (Exception e) {
                            context.getFailureDescription().set(e.getLocalizedMessage());
                        }

                    } // else the subsystem isn't started yet

                    if (!context.hasFailureDescription()) {
                        context.getResult();
                    }

                    context.completeStep(new OperationContext.RollbackHandler() {
                        @Override
                        public void handleRollback(OperationContext context, ModelNode operation) {
                            if (jmsServerService != null) {
                                JMSServerManager jmsServerManager = JMSServerManager.class.cast(jmsServerService.getValue());

                                try {
                                    if (addOperation) {
                                        removeJndiName(jmsServerManager, resourceName, jndiName);
                                    } else {
                                        addJndiName(jmsServerManager, resourceName, jndiName);
                                    }
                                } catch (Exception e) {
                                    context.getFailureDescription().set(e.getLocalizedMessage());
                                }
                            }
                        }
                    });
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }

    protected abstract void addJndiName(JMSServerManager jmsServerManager, String resourceName, String jndiName) throws Exception;

    protected abstract void removeJndiName(JMSServerManager jmsServerManager, String resourceName, String jndiName) throws Exception;
}
