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

import static org.jboss.as.messaging.CommonAttributes.FILTER;
import static org.jboss.as.messaging.MessagingLogger.ROOT_LOGGER;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.EnumSet;
import java.util.Locale;

import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Base class for handlers that interact with either a HornetQ {@link org.hornetq.api.core.management.QueueControl}
 * or a {@link org.hornetq.api.jms.management.JMSQueueControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class AbstractQueueControlHandler<T> extends AbstractRuntimeOnlyHandler {

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

    public static final String MESSAGE_ID = "message-id";
    public static final String NEW_PRIORITY = "new-priority";
    public static final String OTHER_QUEUE_NAME = "other-queue-name";
    public static final String REJECT_DUPLICATES = "reject-duplicates";

    private final ParametersValidator singleOptionalFilterValidator = new ParametersValidator();
    private final ParametersValidator singleMessageIdValidator = new ParametersValidator();
    private final ParametersValidator changeMessagePriorityValidator = new ParametersValidator();
    private final ParametersValidator changeMessagesPriorityValidator = new ParametersValidator();
    private final ParametersValidator moveMessageValidator = new ParametersValidator();
    private final ParametersValidator moveMessagesValidator = new ParametersValidator();

    protected AbstractQueueControlHandler(final ParameterValidator messageIdValidator) {
        //populate validators

        final ParameterValidator filterValidator = new ModelTypeValidator(ModelType.STRING, true, false);
        final ParameterValidator queueNameValidator = new StringLengthValidator(1);
        final ParameterValidator rejectDuplicatesValidator = new ModelTypeValidator(ModelType.BOOLEAN, true);
        final ParameterValidator priorityValidator = new IntRangeValidator(0, 9, false, false);

        singleOptionalFilterValidator.registerValidator(FILTER.getName(), filterValidator);
        singleMessageIdValidator.registerValidator(MESSAGE_ID, messageIdValidator);
        changeMessagePriorityValidator.registerValidator(MESSAGE_ID, messageIdValidator);
        changeMessagePriorityValidator.registerValidator(NEW_PRIORITY, priorityValidator);
        changeMessagesPriorityValidator.registerValidator(FILTER.getName(), filterValidator);
        changeMessagesPriorityValidator.registerValidator(NEW_PRIORITY, priorityValidator);
        moveMessageValidator.registerValidator(MESSAGE_ID, messageIdValidator);
        moveMessageValidator.registerValidator(OTHER_QUEUE_NAME, queueNameValidator);
        moveMessageValidator.registerValidator(REJECT_DUPLICATES, rejectDuplicatesValidator);
        moveMessagesValidator.registerValidator(FILTER.getName(), filterValidator);
        moveMessagesValidator.registerValidator(OTHER_QUEUE_NAME, queueNameValidator);
        moveMessagesValidator.registerValidator(REJECT_DUPLICATES, rejectDuplicatesValidator);
    }

    public void registerOperations(final ManagementResourceRegistration registry) {

        final boolean forJMS = isJMS();

        final EnumSet<OperationEntry.Flag> readOnly = EnumSet.of(OperationEntry.Flag.READ_ONLY, OperationEntry.Flag.RUNTIME_ONLY);

        registry.registerOperationHandler(LIST_MESSAGES, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getListMessages(locale, forJMS, false);
            }
        }, false, OperationEntry.EntryType.PUBLIC, readOnly);

        registry.registerOperationHandler(LIST_MESSAGES_AS_JSON, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getListMessages(locale, forJMS, true);
            }
        }, readOnly);

        registry.registerOperationHandler(COUNT_MESSAGES, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getCountMessages(locale);
            }
        }, readOnly);

        registry.registerOperationHandler(REMOVE_MESSAGE, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getRemoveMessage(locale, forJMS);
            }
        });

        registry.registerOperationHandler(REMOVE_MESSAGES, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getRemoveMessages(locale);
            }
        });

        registry.registerOperationHandler(EXPIRE_MESSAGES, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getExpireMessages(locale);
            }
        });

        registry.registerOperationHandler(EXPIRE_MESSAGE, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getExpireMessage(locale, forJMS);
            }
        });

        registry.registerOperationHandler(SEND_MESSAGE_TO_DEAD_LETTER_ADDRESS, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getSendMessageToDeadLetterAddress(locale, forJMS);
            }
        });

        registry.registerOperationHandler(SEND_MESSAGES_TO_DEAD_LETTER_ADDRESS, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getSendMessagesToDeadLetterAddress(locale);
            }
        });

        registry.registerOperationHandler(CHANGE_MESSAGE_PRIORITY, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getChangeMessagePriority(locale, forJMS);
            }
        });

        registry.registerOperationHandler(CHANGE_MESSAGES_PRIORITY, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getChangeMessagesPriority(locale);
            }
        });

        registry.registerOperationHandler(MOVE_MESSAGE, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getMoveMessage(locale, forJMS);
            }
        });

        registry.registerOperationHandler(MOVE_MESSAGES, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getMoveMessages(locale);
            }
        });

        // TODO dmr-based LIST_MESSAGE_COUNTER

        registry.registerOperationHandler(LIST_MESSAGE_COUNTER_AS_JSON, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleReplyOperation(locale, LIST_MESSAGE_COUNTER_AS_JSON, "queue", ModelType.STRING, true);
            }
        }, readOnly);

        registry.registerOperationHandler(LIST_MESSAGE_COUNTER_AS_HTML, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleReplyOperation(locale, LIST_MESSAGE_COUNTER_AS_HTML, "queue", ModelType.STRING, true);
            }
        }, readOnly);

        registry.registerOperationHandler(RESET_MESSAGE_COUNTER, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getDescriptionOnlyOperation(locale, RESET_MESSAGE_COUNTER, "queue");
            }
        });

        // TODO dmr-based LIST_MESSAGE_COUNTER_HISTORY

        registry.registerOperationHandler(LIST_MESSAGE_COUNTER_HISTORY_AS_JSON, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleReplyOperation(locale, LIST_MESSAGE_COUNTER_HISTORY_AS_JSON, "queue", ModelType.STRING, true);
            }
        }, readOnly);

        registry.registerOperationHandler(LIST_MESSAGE_COUNTER_HISTORY_AS_HTML, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleReplyOperation(locale, LIST_MESSAGE_COUNTER_HISTORY_AS_HTML, "queue", ModelType.STRING, true);
            }
        }, readOnly);

        registry.registerOperationHandler(PAUSE, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getDescriptionOnlyOperation(locale, PAUSE, "queue");
            }
        });

        registry.registerOperationHandler(RESUME, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getDescriptionOnlyOperation(locale, RESUME, "queue");
            }
        });

        // TODO LIST_CONSUMERS

        registry.registerOperationHandler(LIST_CONSUMERS_AS_JSON, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleReplyOperation(locale, LIST_CONSUMERS_AS_JSON, "queue", ModelType.STRING, true);
            }
        }, readOnly);
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String operationName = operation.require(ModelDescriptionConstants.OP).asString();
        final String queueName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> hqService = context.getServiceRegistry(false).getService(hqServiceName);
        HornetQServer hqServer = HornetQServer.class.cast(hqService.getValue());
        DelegatingQueueControl<T> control = getQueueControl(hqServer, queueName);

        boolean reversible = false;
        Object handback = null;
        try {
            if (LIST_MESSAGES.equals(operationName)) {
                singleOptionalFilterValidator.validate(operation);
                String filter = operation.hasDefined(FILTER.getName()) ? operation.get(FILTER.getName()).asString() : null;
                String json = control.listMessagesAsJSON(filter);
                context.getResult().set(ModelNode.fromJSONString(json));
            } else if (LIST_MESSAGES_AS_JSON.equals(operationName)) {
                singleOptionalFilterValidator.validate(operation);
                String filter = operation.hasDefined(FILTER.getName()) ? operation.get(FILTER.getName()).asString() : null;
                context.getResult().set(control.listMessagesAsJSON(filter));
            } else if (COUNT_MESSAGES.equals(operationName)) {
                singleOptionalFilterValidator.validate(operation);
                String filter = operation.hasDefined(FILTER.getName()) ? operation.get(FILTER.getName()).asString() : null;
                context.getResult().set(control.countMessages(filter));
            } else if (REMOVE_MESSAGE.equals(operationName)) {
                singleMessageIdValidator.validate(operation);
                ModelNode id = operation.require(MESSAGE_ID);
                context.getResult().set(control.removeMessage(id));
            } else if (REMOVE_MESSAGES.equals(operationName)) {
                singleOptionalFilterValidator.validate(operation);
                String filter = operation.hasDefined(FILTER.getName()) ? operation.get(FILTER.getName()).asString() : null;
                context.getResult().set(control.removeMessages(filter));
            } else if (EXPIRE_MESSAGES.equals(operationName)) {
                singleOptionalFilterValidator.validate(operation);
                String filter = operation.hasDefined(FILTER.getName()) ? operation.get(FILTER.getName()).asString() : null;
                context.getResult().set(control.expireMessages(filter));
            } else if (EXPIRE_MESSAGE.equals(operationName)) {
                singleMessageIdValidator.validate(operation);
                ModelNode id = operation.require(MESSAGE_ID);
                context.getResult().set(control.expireMessage(id));
            } else if (SEND_MESSAGE_TO_DEAD_LETTER_ADDRESS.equals(operationName)) {
                singleMessageIdValidator.validate(operation);
                ModelNode id = operation.require(MESSAGE_ID);
                context.getResult().set(control.sendMessageToDeadLetterAddress(id));
            } else if (SEND_MESSAGES_TO_DEAD_LETTER_ADDRESS.equals(operationName)) {
                singleOptionalFilterValidator.validate(operation);
                String filter = operation.hasDefined(FILTER.getName()) ? operation.get(FILTER.getName()).asString() : null;
                context.getResult().set(control.sendMessagesToDeadLetterAddress(filter));
            } else if (CHANGE_MESSAGE_PRIORITY.equals(operationName)) {
                changeMessagePriorityValidator.validate(operation);
                ModelNode id = operation.require(MESSAGE_ID);
                int priority = operation.require(NEW_PRIORITY).asInt();
                context.getResult().set(control.changeMessagePriority(id, priority));
            } else if (CHANGE_MESSAGES_PRIORITY.equals(operationName)) {
                changeMessagesPriorityValidator.validate(operation);
                String filter = operation.hasDefined(FILTER.getName()) ? operation.get(FILTER.getName()).asString() : null;
                int priority = operation.require(NEW_PRIORITY).asInt();
                context.getResult().set(control.changeMessagesPriority(filter, priority));
            } else if (MOVE_MESSAGE.equals(operationName)) {
                moveMessageValidator.validate(operation);
                ModelNode id = operation.require(MESSAGE_ID);
                String otherQueue = operation.require(OTHER_QUEUE_NAME).asString();
                if (operation.hasDefined(REJECT_DUPLICATES)) {
                    boolean reject = operation.get(REJECT_DUPLICATES).asBoolean();
                    context.getResult().set(control.moveMessage(id, otherQueue, reject));
                } else {
                    context.getResult().set(control.moveMessage(id, otherQueue));
                }
            } else if (MOVE_MESSAGES.equals(operationName)) {
                moveMessagesValidator.validate(operation);
                String filter = operation.hasDefined(FILTER.getName()) ? operation.get(FILTER.getName()).asString() : null;
                String otherQueue = operation.require(OTHER_QUEUE_NAME).asString();
                if (operation.hasDefined(REJECT_DUPLICATES)) {
                    boolean reject = operation.get(REJECT_DUPLICATES).asBoolean();
                    context.getResult().set(control.moveMessages(filter, otherQueue, reject));
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
                // TODO LIST_MESSAGE_COUNTER, LIST_MESSAGE_COUNTER_HISTORY, LIST_CONSUMERS
                handback = handleAdditionalOperation(operationName, operation, context, control.getDelegate());
                reversible = handback == null;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            context.getFailureDescription().set(e.getLocalizedMessage());
        }

        if (context.completeStep() != OperationContext.ResultAction.KEEP && reversible) {
            try {
                if (PAUSE.equals(operationName)) {
                    control.resume();
                } else if (RESUME.equals(operationName)) {
                    control.pause();
                } else {
                    revertAdditionalOperation(operationName, operation, context, control.getDelegate(), handback);
                }
            } catch (Exception e) {
                ROOT_LOGGER.revertOperationFailed(e, getClass().getSimpleName(),
                        operation.require(ModelDescriptionConstants.OP).asString(),
                        PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)));
            }
        }
    }

    protected abstract DelegatingQueueControl<T> getQueueControl(HornetQServer hqServer, String queueName);

    protected abstract Object handleAdditionalOperation(final String operationName, final ModelNode operation,
                                                        final OperationContext context, T queueControl) throws OperationFailedException;

    protected abstract void revertAdditionalOperation(final String operationName, final ModelNode operation,
                                                      final OperationContext context, T queueControl, Object handback);

    protected abstract boolean isJMS();

    protected final void throwUnimplementedOperationException(final String operationName) {
        // Bug
        throw MESSAGES.unsupportedOperation(operationName);
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
