/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.injection.deployment;


import java.io.Serializable;

import jakarta.jms.BytesMessage;
import jakarta.jms.ConnectionMetaData;
import jakarta.jms.Destination;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.StreamMessage;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TemporaryTopic;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import org.wildfly.extension.messaging.activemq.injection._private.MessagingLogger;


/**
 * Wrapper to restrict use of methods for injected JMSContext (Jakarta Messaging 2.0 spec, §12.4.5).
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
