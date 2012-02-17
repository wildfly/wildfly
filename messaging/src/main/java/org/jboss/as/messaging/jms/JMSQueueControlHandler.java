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

package org.jboss.as.messaging.jms;

import org.hornetq.api.core.management.ResourceNames;
import org.hornetq.api.jms.management.JMSQueueControl;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.messaging.AbstractQueueControlHandler;
import org.jboss.dmr.ModelNode;

/**
 * Handler for runtime operations that invoke on a HornetQ {@link org.hornetq.api.jms.management.JMSQueueControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JMSQueueControlHandler extends AbstractQueueControlHandler<JMSQueueControl> {

    public static final JMSQueueControlHandler INSTANCE = new JMSQueueControlHandler();

    private JMSQueueControlHandler() {
        super(new StringLengthValidator(1));
    }

    @Override
    public boolean isJMS() {
        return true;
    }

    protected AbstractQueueControlHandler.DelegatingQueueControl<JMSQueueControl> getQueueControl(HornetQServer hqServer, String queueName){
        final JMSQueueControl control = JMSQueueControl.class.cast(hqServer.getManagementService().getResource(ResourceNames.JMS_QUEUE + queueName));
        return new AbstractQueueControlHandler.DelegatingQueueControl<JMSQueueControl>() {

            @Override
            public JMSQueueControl getDelegate() {
                return  control;
            }

            @Override
            public String listMessagesAsJSON(String filter) throws Exception {
                return control.listMessagesAsJSON(filter);
            }

            @Override
            public long countMessages(String filter) throws Exception {
                return control.countMessages(filter);
            }

            @Override
            public boolean removeMessage(ModelNode id) throws Exception {
                return control.removeMessage(id.asString());
            }

            @Override
            public int removeMessages(String filter) throws Exception {
                return control.removeMessages(filter);
            }

            @Override
            public int expireMessages(String filter) throws Exception {
                return control.expireMessages(filter);
            }

            @Override
            public boolean expireMessage(ModelNode id) throws Exception {
                return control.expireMessage(id.asString());
            }

            @Override
            public boolean sendMessageToDeadLetterAddress(ModelNode id) throws Exception {
                return control.sendMessageToDeadLetterAddress(id.asString());
            }

            @Override
            public int sendMessagesToDeadLetterAddress(String filter) throws Exception {
                return control.sendMessagesToDeadLetterAddress(filter);
            }

            @Override
            public boolean changeMessagePriority(ModelNode id, int priority) throws Exception {
                return control.changeMessagePriority(id.asString(), priority);
            }

            @Override
            public int changeMessagesPriority(String filter, int priority) throws Exception {
                return control.changeMessagesPriority(filter, priority);
            }

            @Override
            public boolean moveMessage(ModelNode id, String otherQueue) throws Exception {
                return control.moveMessage(id.asString(), otherQueue);
            }

            @Override
            public boolean moveMessage(ModelNode id, String otherQueue, boolean rejectDuplicates) throws Exception {
                return control.moveMessage(id.asString(), otherQueue, rejectDuplicates);
            }

            @Override
            public int moveMessages(String filter, String otherQueue) throws Exception {
                return control.moveMessages(filter, otherQueue);
            }

            @Override
            public int moveMessages(String filter, String otherQueue, boolean rejectDuplicates) throws Exception {
                return control.moveMessages(filter, otherQueue, rejectDuplicates);
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
        };
    }

    @Override
    protected Object handleAdditionalOperation(String operationName, ModelNode operation, OperationContext context,
                                               JMSQueueControl queueControl) throws OperationFailedException {
        throwUnimplementedOperationException(operationName);
        return null;
    }

    @Override
    protected void revertAdditionalOperation(String operationName, ModelNode operation, OperationContext context, JMSQueueControl queueControl, Object handback) {
        // no-op
    }
}
