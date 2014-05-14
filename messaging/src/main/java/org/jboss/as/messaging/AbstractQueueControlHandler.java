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

package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.messaging.CommonAttributes.FILTER;
import static org.jboss.as.messaging.CommonAttributes.QUEUE;
import static org.jboss.as.messaging.HornetQActivationService.rollbackOperationIfServerNotActive;
import static org.jboss.as.messaging.OperationDefinitionHelper.createNonEmptyStringAttribute;
import static org.jboss.as.messaging.OperationDefinitionHelper.resolveFilter;
import static org.jboss.as.messaging.OperationDefinitionHelper.runtimeOnlyOperation;
import static org.jboss.as.messaging.OperationDefinitionHelper.runtimeReadOnlyOperation;
import static org.jboss.as.messaging.logging.MessagingLogger.ROOT_LOGGER;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LIST;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;

import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Base class for handlers that interact with either a HornetQ {@link org.hornetq.api.core.management.QueueControl}
 * or a {@link org.hornetq.api.jms.management.JMSQueueControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2014 Red Hat Inc.
 */
public abstract class AbstractQueueControlHandler<T> extends AbstractRuntimeOnlyHandler {

    private static ResourceDescriptionResolver RESOLVER = MessagingExtension.getResourceDescriptionResolver(QUEUE);

    public static final String LIST_MESSAGES = "list-messages";
    public static final String LIST_MESSAGES_AS_JSON = "list-messages-as-json";
    public static final String COUNT_MESSAGES = "count-messages";
    public static final String REMOVE_MESSAGE = "remove-message";
    public static final String REMOVE_MESSAGES = "remove-messages";
    public static final String EXPIRE_MESSAGES = "expire-messages";
    public static final String EXPIRE_MESSAGE = "expire-message";
    public static final String SEND_MESSAGE_TO_DEAD_LETTER_ADDRESS = "send-message-to-dead-letter-address";
    public static final String SEND_MESSAGES_TO_DEAD_LETTER_ADDRESS = "send-messages-to-dead-letter-address";
    public static final String CHANGE_MESSAGE_PRIORITY = "change-message-priority";
    public static final String CHANGE_MESSAGES_PRIORITY = "change-messages-priority";
    public static final String MOVE_MESSAGE = "move-message";
    public static final String MOVE_MESSAGES = "move-messages";
    public static final String LIST_MESSAGE_COUNTER = "list-message-counter";
    public static final String LIST_MESSAGE_COUNTER_AS_JSON = "list-message-counter-as-json";
    public static final String LIST_MESSAGE_COUNTER_AS_HTML = "list-message-counter-as-html";
    public static final String RESET_MESSAGE_COUNTER = "reset-message-counter";
    public static final String LIST_MESSAGE_COUNTER_HISTORY = "list-message-counter-history";
    public static final String LIST_MESSAGE_COUNTER_HISTORY_AS_JSON = "list-message-counter-history-as-json";
    public static final String LIST_MESSAGE_COUNTER_HISTORY_AS_HTML = "list-message-counter-history-as-html";
    public static final String PAUSE = "pause";
    public static final String RESUME = "resume";
    public static final String LIST_CONSUMERS = "list-consumers";
    public static final String LIST_CONSUMERS_AS_JSON = "list-consumers-as-json";

    public static final ParameterValidator PRIORITY_VALIDATOR = new IntRangeValidator(0, 9, false, false);

    private static final AttributeDefinition OTHER_QUEUE_NAME = createNonEmptyStringAttribute("other-queue-name");
    private static final AttributeDefinition REJECT_DUPLICATES = SimpleAttributeDefinitionBuilder.create("reject-duplicates", BOOLEAN)
            .setAllowNull(true)
            .build();
    private static final AttributeDefinition NEW_PRIORITY = SimpleAttributeDefinitionBuilder.create("new-priority", INT)
            .setValidator(PRIORITY_VALIDATOR)
            .build();

    protected abstract AttributeDefinition getMessageIDAttributeDefinition();

    protected abstract AttributeDefinition[] getReplyMessageParameterDefinitions();

    public void registerOperations(final ManagementResourceRegistration registry, ResourceDescriptionResolver resolver) {

        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_MESSAGES, resolver)
                .setParameters(FILTER)
                .setReplyType(LIST)
                .setReplyParameters(getReplyMessageParameterDefinitions())
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_MESSAGES_AS_JSON, RESOLVER)
                .setParameters(FILTER)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(COUNT_MESSAGES, RESOLVER)
                .setParameters(FILTER)
                .setReplyType(LONG)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(REMOVE_MESSAGE, RESOLVER)
                .setParameters(getMessageIDAttributeDefinition())
                .setReplyType(BOOLEAN)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(REMOVE_MESSAGES, RESOLVER)
                .setParameters(CommonAttributes.FILTER)
                .setReplyType(INT)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(EXPIRE_MESSAGE, RESOLVER)
                .setParameters(getMessageIDAttributeDefinition())
                .setReplyType(BOOLEAN)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(EXPIRE_MESSAGES, RESOLVER)
                .setParameters(FILTER)
                .setReplyType(INT)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(SEND_MESSAGE_TO_DEAD_LETTER_ADDRESS, RESOLVER)
                .setParameters(getMessageIDAttributeDefinition())
                .setReplyType(BOOLEAN)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(SEND_MESSAGES_TO_DEAD_LETTER_ADDRESS, RESOLVER)
                .setParameters(FILTER)
                .setReplyType(INT)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(CHANGE_MESSAGE_PRIORITY, RESOLVER)
                .setParameters(getMessageIDAttributeDefinition(), NEW_PRIORITY)
                .setReplyType(BOOLEAN)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(CHANGE_MESSAGES_PRIORITY, RESOLVER)
                .setParameters(FILTER, NEW_PRIORITY)
                .setReplyType(INT)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(MOVE_MESSAGE, RESOLVER)
                .setParameters(getMessageIDAttributeDefinition(), OTHER_QUEUE_NAME, REJECT_DUPLICATES)
                .setReplyType(INT)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(MOVE_MESSAGES, RESOLVER)
                .setParameters(FILTER, OTHER_QUEUE_NAME, REJECT_DUPLICATES)
                .setReplyType(INT)
                .build(),
                this);

        // TODO dmr-based LIST_MESSAGE_COUNTER

        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_MESSAGE_COUNTER_AS_JSON, RESOLVER)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_MESSAGE_COUNTER_AS_HTML, RESOLVER)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(RESET_MESSAGE_COUNTER, RESOLVER)
                .build(),
                this);

        // TODO dmr-based LIST_MESSAGE_COUNTER_HISTORY

        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_MESSAGE_COUNTER_HISTORY_AS_JSON, RESOLVER)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_MESSAGE_COUNTER_HISTORY_AS_HTML, RESOLVER)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(PAUSE, RESOLVER)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(RESUME, RESOLVER)
                .build(),
                this);

        // TODO LIST_CONSUMERS

        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_CONSUMERS_AS_JSON, RESOLVER)
                .setReplyType(STRING)
                .build(),
                this);
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        if (rollbackOperationIfServerNotActive(context, operation)) {
            return;
        }

        final String operationName = operation.require(ModelDescriptionConstants.OP).asString();
        final String queueName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> hqService = context.getServiceRegistry(false).getService(hqServiceName);
        HornetQServer hqServer = HornetQServer.class.cast(hqService.getValue());
        final DelegatingQueueControl<T> control = getQueueControl(hqServer, queueName);

        if (control == null) {
            PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            throw ControllerLogger.ROOT_LOGGER.managementResourceNotFound(address);
        }

        boolean reversible = false;
        Object handback = null;
        try {
            if (LIST_MESSAGES.equals(operationName)) {
                String filter = resolveFilter(context, operation);
                String json = control.listMessagesAsJSON(filter);
                context.getResult().set(ModelNode.fromJSONString(json));
            } else if (LIST_MESSAGES_AS_JSON.equals(operationName)) {
                String filter = resolveFilter(context, operation);
                context.getResult().set(control.listMessagesAsJSON(filter));
            } else if (COUNT_MESSAGES.equals(operationName)) {
                String filter = resolveFilter(context, operation);
                context.getResult().set(control.countMessages(filter));
            } else if (REMOVE_MESSAGE.equals(operationName)) {
                ModelNode id = getMessageIDAttributeDefinition().resolveModelAttribute(context, operation);
                context.getResult().set(control.removeMessage(id));
            } else if (REMOVE_MESSAGES.equals(operationName)) {
                String filter = resolveFilter(context, operation);
                context.getResult().set(control.removeMessages(filter));
            } else if (EXPIRE_MESSAGES.equals(operationName)) {
                String filter = resolveFilter(context, operation);
                context.getResult().set(control.expireMessages(filter));
            } else if (EXPIRE_MESSAGE.equals(operationName)) {
                ModelNode id = getMessageIDAttributeDefinition().resolveModelAttribute(context, operation);
                context.getResult().set(control.expireMessage(id));
            } else if (SEND_MESSAGE_TO_DEAD_LETTER_ADDRESS.equals(operationName)) {
                ModelNode id = getMessageIDAttributeDefinition().resolveModelAttribute(context, operation);
                context.getResult().set(control.sendMessageToDeadLetterAddress(id));
            } else if (SEND_MESSAGES_TO_DEAD_LETTER_ADDRESS.equals(operationName)) {
                String filter = resolveFilter(context, operation);
                context.getResult().set(control.sendMessagesToDeadLetterAddress(filter));
            } else if (CHANGE_MESSAGE_PRIORITY.equals(operationName)) {
                ModelNode id = getMessageIDAttributeDefinition().resolveModelAttribute(context, operation);
                int priority = NEW_PRIORITY.resolveModelAttribute(context, operation).asInt();
                context.getResult().set(control.changeMessagePriority(id, priority));
            } else if (CHANGE_MESSAGES_PRIORITY.equals(operationName)) {
                String filter = resolveFilter(context, operation);
                int priority = NEW_PRIORITY.resolveModelAttribute(context, operation).asInt();
                context.getResult().set(control.changeMessagesPriority(filter, priority));
            } else if (MOVE_MESSAGE.equals(operationName)) {
                ModelNode id = getMessageIDAttributeDefinition().resolveModelAttribute(context, operation);
                String otherQueue = OTHER_QUEUE_NAME.resolveModelAttribute(context, operation).asString();
                ModelNode rejectDuplicates = REJECT_DUPLICATES.resolveModelAttribute(context, operation);
                if (rejectDuplicates.isDefined()) {
                    context.getResult().set(control.moveMessage(id, otherQueue, rejectDuplicates.asBoolean()));
                } else {
                    context.getResult().set(control.moveMessage(id, otherQueue));
                }
            } else if (MOVE_MESSAGES.equals(operationName)) {
                String filter = resolveFilter(context, operation);
                String otherQueue = OTHER_QUEUE_NAME.resolveModelAttribute(context, operation).asString();
                ModelNode rejectDuplicates = REJECT_DUPLICATES.resolveModelAttribute(context, operation);
                if (rejectDuplicates.isDefined()) {
                    context.getResult().set(control.moveMessages(filter, otherQueue, rejectDuplicates.asBoolean()));
                } else {
                    context.getResult().set(control.moveMessages(filter, otherQueue));
                }
            } else if (LIST_MESSAGE_COUNTER_AS_JSON.equals(operationName)) {
                context.getResult().set(control.listMessageCounter());
            } else if (LIST_MESSAGE_COUNTER_AS_HTML.equals(operationName)) {
                context.getResult().set(control.listMessageCounterAsHTML());
            } else if (LIST_MESSAGE_COUNTER_HISTORY_AS_JSON.equals(operationName)) {
                context.getResult().set(control.listMessageCounterHistory());
            } else if (LIST_MESSAGE_COUNTER_HISTORY_AS_HTML.equals(operationName)) {
                context.getResult().set(control.listMessageCounterHistoryAsHTML());
            } else if (RESET_MESSAGE_COUNTER.equals(operationName)) {
                control.resetMessageCounter();
                context.getResult(); // undefined
            } else if (PAUSE.equals(operationName)) {
                control.pause();
                reversible = true;
                context.getResult(); // undefined
            } else if (RESUME.equals(operationName)) {
                control.resume();
                reversible = true;
                context.getResult(); // undefined
            } else if (LIST_CONSUMERS_AS_JSON.equals(operationName)) {
                context.getResult().set(control.listConsumersAsJSON());
            } else {
                // TODO dmr-based LIST_MESSAGE_COUNTER, LIST_MESSAGE_COUNTER_HISTORY, LIST_CONSUMERS
                handback = handleAdditionalOperation(operationName, operation, context, control.getDelegate());
                reversible = handback == null;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            context.getFailureDescription().set(e.getLocalizedMessage());
        }

        OperationContext.RollbackHandler rh;
        if (reversible) {
            final Object rhHandback = handback;
            rh = new OperationContext.RollbackHandler() {
                @Override
                public void handleRollback(OperationContext context, ModelNode operation) {
                    try {
                        if (PAUSE.equals(operationName)) {
                            control.resume();
                        } else if (RESUME.equals(operationName)) {
                            control.pause();
                        } else {
                            revertAdditionalOperation(operationName, operation, context, control.getDelegate(), rhHandback);
                        }
                    } catch (Exception e) {
                        ROOT_LOGGER.revertOperationFailed(e, getClass().getSimpleName(),
                                operation.require(ModelDescriptionConstants.OP).asString(),
                                PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)));
                    }
                }
            };
        } else {
            rh = OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER;
        }

        context.completeStep(rh);
    }

    protected abstract DelegatingQueueControl<T> getQueueControl(HornetQServer hqServer, String queueName);

    protected abstract Object handleAdditionalOperation(final String operationName, final ModelNode operation,
                                                        final OperationContext context, T queueControl) throws OperationFailedException;

    protected abstract void revertAdditionalOperation(final String operationName, final ModelNode operation,
                                                      final OperationContext context, T queueControl, Object handback);

    protected final void throwUnimplementedOperationException(final String operationName) {
        // Bug
        throw MessagingLogger.ROOT_LOGGER.unsupportedOperation(operationName);
    }

    /**
     * Exposes the method signatures that are common between {@link org.hornetq.api.core.management.QueueControl}
     * and {@link org.hornetq.api.jms.management.JMSQueueControl}.
     */
    public interface DelegatingQueueControl<T> {

        T getDelegate();

        String listMessagesAsJSON(String filter) throws Exception;

        long countMessages(String filter) throws Exception;

        boolean removeMessage(ModelNode id) throws Exception;

        int removeMessages(String filter) throws Exception;

        int expireMessages(String filter) throws Exception;

        boolean expireMessage(ModelNode id) throws Exception;

        boolean sendMessageToDeadLetterAddress(ModelNode id) throws Exception;

        int sendMessagesToDeadLetterAddress(String filter) throws Exception;

        boolean changeMessagePriority(ModelNode id, int priority) throws Exception;

        int changeMessagesPriority(String filter, int priority) throws Exception;

        boolean moveMessage(ModelNode id, String otherQueue) throws Exception;

        boolean moveMessage(ModelNode id, String otherQueue, boolean rejectDuplicates) throws Exception;

        int moveMessages(String filter, String otherQueue) throws Exception;

        int moveMessages(String filter, String otherQueue, boolean rejectDuplicates) throws Exception;

        String listMessageCounter() throws Exception;

        void resetMessageCounter() throws Exception;

        String listMessageCounterAsHTML() throws Exception;

        String listMessageCounterHistory() throws Exception;

        String listMessageCounterHistoryAsHTML() throws Exception;

        void pause() throws Exception;

        void resume() throws Exception;

        String listConsumersAsJSON() throws Exception;
    }
}
