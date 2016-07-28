/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq.deployment.injection;


import java.io.Serializable;

import javax.jms.BytesMessage;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * Wrapper to restrict use of methods for injected JMSContext (JMS 2.0 spec, §12.4.5).
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
abstract class JMSContextWrapper implements JMSContext {

    abstract JMSContext getDelegate();

    // JMSContext interface implementation

    @Override
    public JMSContext createContext(int sessionMode) {
        return getDelegate().createContext(sessionMode);
    }

    @Override
    public JMSProducer createProducer() {
        return getDelegate().createProducer();
    }

    @Override
    public String getClientID() {
        return getDelegate().getClientID();
    }

    @Override
    public void setClientID(String clientID) {
        throw MessagingLogger.ROOT_LOGGER.callNotPermittedOnInjectedJMSContext();
    }

    @Override
    public ConnectionMetaData getMetaData() {
        return getDelegate().getMetaData();
    }

    @Override
    public ExceptionListener getExceptionListener() {
        return getDelegate().getExceptionListener();
    }

    @Override
    public void setExceptionListener(ExceptionListener listener) {
        throw MessagingLogger.ROOT_LOGGER.callNotPermittedOnInjectedJMSContext();
    }

    @Override
    public void start() {
        throw MessagingLogger.ROOT_LOGGER.callNotPermittedOnInjectedJMSContext();
    }

    @Override

    public void stop() {
        throw MessagingLogger.ROOT_LOGGER.callNotPermittedOnInjectedJMSContext();
    }

    @Override
    public void setAutoStart(boolean autoStart) {
        throw MessagingLogger.ROOT_LOGGER.callNotPermittedOnInjectedJMSContext();
    }

    @Override
    public boolean getAutoStart() {
        return getDelegate().getAutoStart();
    }

    @Override
    public void close() {
        throw MessagingLogger.ROOT_LOGGER.callNotPermittedOnInjectedJMSContext();
    }

    @Override
    public BytesMessage createBytesMessage() {
        return getDelegate().createBytesMessage();
    }

    @Override
    public MapMessage createMapMessage() {
        return getDelegate().createMapMessage();
    }

    @Override
    public Message createMessage() {
        return getDelegate().createMessage();
    }

    @Override
    public ObjectMessage createObjectMessage() {
        return getDelegate().createObjectMessage();
    }

    @Override
    public ObjectMessage createObjectMessage(Serializable object) {
        return getDelegate().createObjectMessage(object);
    }

    @Override
    public StreamMessage createStreamMessage() {
        return getDelegate().createStreamMessage();
    }

    @Override
    public TextMessage createTextMessage() {
        return getDelegate().createTextMessage();
    }

    @Override
    public TextMessage createTextMessage(String text) {
        return getDelegate().createTextMessage(text);
    }

    @Override
    public boolean getTransacted() {
        return getDelegate().getTransacted();
    }

    @Override
    public int getSessionMode() {
        return getDelegate().getSessionMode();
    }

    @Override
    public void commit() {
        throw MessagingLogger.ROOT_LOGGER.callNotPermittedOnInjectedJMSContext();
    }

    @Override
    public void rollback() {
        throw MessagingLogger.ROOT_LOGGER.callNotPermittedOnInjectedJMSContext();
    }

    @Override
    public void recover() {
        throw MessagingLogger.ROOT_LOGGER.callNotPermittedOnInjectedJMSContext();
    }

    @Override
    public JMSConsumer createConsumer(Destination destination) {
        return getDelegate().createConsumer(destination);
    }

    @Override
    public JMSConsumer createConsumer(Destination destination, String messageSelector) {
        return getDelegate().createConsumer(destination, messageSelector);
    }

    @Override
    public JMSConsumer createConsumer(Destination destination, String messageSelector, boolean noLocal) {
        return getDelegate().createConsumer(destination, messageSelector, noLocal);
    }

    @Override
    public Queue createQueue(String queueName) {
        return getDelegate().createQueue(queueName);
    }

    @Override
    public Topic createTopic(String topicName) {
        return getDelegate().createTopic(topicName);
    }

    @Override
    public JMSConsumer createDurableConsumer(Topic topic, String name) {
        return getDelegate().createDurableConsumer(topic, name);
    }

    @Override
    public JMSConsumer createDurableConsumer(Topic topic, String name, String messageSelector, boolean noLocal) {
        return getDelegate().createDurableConsumer(topic, name, messageSelector, noLocal);
    }

    @Override
    public JMSConsumer createSharedDurableConsumer(Topic topic, String name) {
        return getDelegate().createSharedDurableConsumer(topic, name);
    }

    @Override
    public JMSConsumer createSharedDurableConsumer(Topic topic, String name, String messageSelector) {
        return getDelegate().createSharedDurableConsumer(topic, name, messageSelector);
    }

    @Override
    public JMSConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName) {
        return getDelegate().createSharedConsumer(topic, sharedSubscriptionName);
    }

    @Override
    public JMSConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName, String messageSelector) {
        return getDelegate().createSharedConsumer(topic, sharedSubscriptionName, messageSelector);
    }

    @Override
    public QueueBrowser createBrowser(Queue queue) {
        return getDelegate().createBrowser(queue);
    }

    @Override
    public QueueBrowser createBrowser(Queue queue, String messageSelector) {
        return getDelegate().createBrowser(queue, messageSelector);
    }

    @Override
    public TemporaryQueue createTemporaryQueue() {
        return getDelegate().createTemporaryQueue();
    }

    @Override
    public TemporaryTopic createTemporaryTopic() {
        return getDelegate().createTemporaryTopic();
    }

    @Override
    public void unsubscribe(String name) {
        getDelegate().unsubscribe(name);
    }

    @Override
    public void acknowledge() {
        throw MessagingLogger.ROOT_LOGGER.callNotPermittedOnInjectedJMSContext();
    }
}
