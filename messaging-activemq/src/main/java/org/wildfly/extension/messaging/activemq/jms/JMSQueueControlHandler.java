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

import static org.apache.activemq.artemis.utils.SelectorTranslator.convertToActiveMQFilterString;
import static org.wildfly.extension.messaging.activemq.OperationDefinitionHelper.createNonEmptyStringAttribute;

import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq.AbstractQueueControlHandler;

/**
 * Handler for runtime operations that invoke on a ActiveMQ {@link QueueControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JMSQueueControlHandler extends AbstractQueueControlHandler<QueueControl> {

    public static final JMSQueueControlHandler INSTANCE = new JMSQueueControlHandler();

    private static final AttributeDefinition MESSAGE_ID = createNonEmptyStringAttribute("message-id");

    private JMSQueueControlHandler() {
    }

    @Override
    protected AttributeDefinition getMessageIDAttributeDefinition() {
        return MESSAGE_ID;
    }

    @Override
    protected AttributeDefinition[] getReplyMessageParameterDefinitions() {
        return JMSManagementHelper.JMS_MESSAGE_PARAMETERS;
    }

    protected AbstractQueueControlHandler.DelegatingQueueControl<QueueControl> getQueueControl(ActiveMQServer server, String queueName){
        final QueueControl control = QueueControl.class.cast(server.getManagementService().getResource(ResourceNames.QUEUE + queueName));
        if (control == null) {
            return null;
        }
        return new AbstractQueueControlHandler.DelegatingQueueControl<QueueControl>() {

            @Override
            public QueueControl getDelegate() {
                return  control;
            }

            @Override
            public String listMessagesAsJSON(String filter) throws Exception {
                String result = control.listMessagesAsJSON(convertToActiveMQFilterString(filter));
                return convertToJMSProperties(result);
            }

            @Override
            public long countMessages(String filter) throws Exception {
                return control.countMessages(convertToActiveMQFilterString(filter));
            }

            @Override
            public boolean removeMessage(ModelNode id) throws Exception {
                int n = control.removeMessages(createFilterForJMSMessageID(id));
                return n == 1;
            }

            @Override
            public int removeMessages(String filter) throws Exception {
                return control.removeMessages(convertToActiveMQFilterString(filter));
            }

            @Override
            public int expireMessages(String filter) throws Exception {
                return control.expireMessages(convertToActiveMQFilterString(filter));
            }

            @Override
            public boolean expireMessage(ModelNode id) throws Exception {
                int n = control.expireMessages(createFilterForJMSMessageID(id));
                return n == 1;
            }

            @Override
            public boolean sendMessageToDeadLetterAddress(ModelNode id) throws Exception {
                int n = control.sendMessagesToDeadLetterAddress(createFilterForJMSMessageID(id));
                return n == 1;
            }

            @Override
            public int sendMessagesToDeadLetterAddress(String filter) throws Exception {
                return control.sendMessagesToDeadLetterAddress(convertToActiveMQFilterString(filter));
            }

            @Override
            public boolean changeMessagePriority(ModelNode id, int priority) throws Exception {
                int n = control.changeMessagesPriority(createFilterForJMSMessageID(id), priority);
                return n == 1;
            }

            @Override
            public int changeMessagesPriority(String filter, int priority) throws Exception {
                return control.changeMessagesPriority(convertToActiveMQFilterString(filter), priority);
            }

            @Override
            public boolean moveMessage(ModelNode id, String otherQueue) throws Exception {
                int n = control.moveMessages(createFilterForJMSMessageID(id), otherQueue);
                return n == 1;
            }

            @Override
            public boolean moveMessage(ModelNode id, String otherQueue, boolean rejectDuplicates) throws Exception {
                int n = control.moveMessages(createFilterForJMSMessageID(id), otherQueue, rejectDuplicates);
                return n == 1;
            }

            @Override
            public int moveMessages(String filter, String otherQueue) throws Exception {
                return control.moveMessages(convertToActiveMQFilterString(filter), otherQueue);
            }

            @Override
            public int moveMessages(String filter, String otherQueue, boolean rejectDuplicates) throws Exception {
                return control.moveMessages(convertToActiveMQFilterString(filter), otherQueue, rejectDuplicates);
            }

            @Override
            public String listMessageCounter() throws Exception {
                return control.listMessageCounter();
            }

            @Override
            public void resetMessageCounter() throws Exception {
                control.resetMessageCounter();
            }

            @Override
            public String listMessageCounterAsHTML() throws Exception {
                return control.listMessageCounterAsHTML();
            }

            @Override
            public String listMessageCounterHistory() throws Exception {
                return control.listMessageCounterHistory();
            }

            @Override
            public String listMessageCounterHistoryAsHTML() throws Exception {
                return control.listMessageCounterHistoryAsHTML();
            }

            @Override
            public void pause() throws Exception {
                control.pause();
            }

            @Override
            public void resume() throws Exception {
                control.resume();
            }

            @Override
            public String listConsumersAsJSON() throws Exception {
                return control.listConsumersAsJSON();
            }

            @Override
            public String listScheduledMessagesAsJSON() throws Exception {
                return control.listScheduledMessagesAsJSON();
            }

            @Override
            public String listDeliveringMessagesAsJSON() throws Exception {
                return control.listDeliveringMessagesAsJSON();
            }

            private String createFilterForJMSMessageID(ModelNode id) {
                return "AMQUserID='" + id.asString() + "'";
            }

            private String convertToJMSProperties(String text) {
                return text.replaceAll("priority", "JMSPriority")
                        .replaceAll("timestamp", "JMSTimestamp")
                        .replaceAll("expiration", "JMSExpiration")
                        .replaceAll("durable", "JMSDeliveryMode")
                        .replaceAll("userID", "JMSMessageID");
            }
        };
    }

    @Override
    protected Object handleAdditionalOperation(String operationName, ModelNode operation, OperationContext context,
                                               QueueControl queueControl) throws OperationFailedException {
        throwUnimplementedOperationException(operationName);
        return null;
    }

    @Override
    protected void revertAdditionalOperation(String operationName, ModelNode operation, OperationContext context, QueueControl queueControl, Object handback) {
        // no-op
    }
}
