/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.deployment;

import static javax.jms.JMSContext.AUTO_ACKNOWLEDGE;

import java.io.Serializable;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSPasswordCredential;
import javax.jms.JMSProducer;
import javax.jms.JMSSessionMode;
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
import javax.jms.XAConnectionFactory;
import javax.jms.XAJMSContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Producer factory for JMSContext resources.
 *
 * => Within the same scope, different injected JMSContext objects which are injected using identical annotations will all refer to the same JMSContext object.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class JMSContextProducer {

    private static final String TRANSACTION_SYNCHRONIZATION_REGISTRY_LOOKUP = "java:comp/TransactionSynchronizationRegistry";
    private static final Object TRANSACTION_KEY = "TRANSACTED_JMS_CONTEXT";

    /**
     * the propertyReplace is set in {@link org.jboss.as.messaging.deployment.JMSCDIExtension#wrapInjectionTarget(javax.enterprise.inject.spi.ProcessInjectionTarget)}.
     */
    private PropertyReplacer propertyReplacer;

    public JMSContextProducer() {
    }

    void setPropertyReplacer(PropertyReplacer propertyReplacer) {
        this.propertyReplacer = propertyReplacer;
    }

    /**
     * CDI Producer method for injected {@link JMSContext}.
     */
    @Produces
    public JMSContext getJMSContext(InjectionPoint injectionPoint) throws NamingException {
        String connectionFactoryLookup = DefaultJMSConnectionFactoryBindingProcessor.COMP_DEFAULT_JMS_CONNECTION_FACTORY;
        String userName = null;
        String password = null;
        int ackMode = AUTO_ACKNOWLEDGE;

        if (injectionPoint != null) {
            // Check for @JMSConnectionFactory annotation
            if (injectionPoint.getAnnotated().isAnnotationPresent(JMSConnectionFactory.class)) {
                JMSConnectionFactory cf = injectionPoint.getAnnotated().getAnnotation(JMSConnectionFactory.class);
                connectionFactoryLookup = propertyReplacer.replaceProperties(cf.value());
            }

            // Check for JMSPasswordCredential annotation
            if (injectionPoint.getAnnotated().isAnnotationPresent(JMSPasswordCredential.class)) {
                JMSPasswordCredential credential = injectionPoint.getAnnotated().getAnnotation(JMSPasswordCredential.class);
                userName = propertyReplacer.replaceProperties(credential.userName());
                password = propertyReplacer.replaceProperties(credential.password());
            }

            // Check for JMSSessionMode annotation
            if (injectionPoint.getAnnotated().isAnnotationPresent(JMSSessionMode.class)) {
                JMSSessionMode sessionMode = injectionPoint.getAnnotated().getAnnotation(JMSSessionMode.class);
                ackMode = sessionMode.value();
            }
        }

        JMSInfo info = new JMSInfo(connectionFactoryLookup, userName, password, ackMode);

        return new JMSContextWrapper(info);
    }

    /**
     * CDI disposable method for injected {@link JMSContext}.
     */
    public void closeJMSContext(@Disposes JMSContext context) {
        if (context instanceof JMSContextWrapper) {
            // close on the delegate context, the wrapper throwing an exception in its close() method.
            ((JMSContextWrapper)context).internalClose();
        }
    }

    private final class JMSInfo {
        private final String connectionFactoryLookup;
        private final String userName;
        private final String password;
        private final int ackMode;

        JMSInfo(String connectionFactoryLookup, String userName, String password, int ackMode) {
            this.connectionFactoryLookup = connectionFactoryLookup;
            this.userName = userName;
            this.password = password;
            this.ackMode = ackMode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            JMSInfo jmsInfo = (JMSInfo) o;

            if (ackMode != jmsInfo.ackMode) return false;
            if (connectionFactoryLookup != null ? !connectionFactoryLookup.equals(jmsInfo.connectionFactoryLookup) : jmsInfo.connectionFactoryLookup != null)
                return false;
            if (password != null ? !password.equals(jmsInfo.password) : jmsInfo.password != null) return false;
            if (userName != null ? !userName.equals(jmsInfo.userName) : jmsInfo.userName != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = connectionFactoryLookup != null ? connectionFactoryLookup.hashCode() : 0;
            result = 31 * result + (userName != null ? userName.hashCode() : 0);
            result = 31 * result + (password != null ? password.hashCode() : 0);
            result = 31 * result + ackMode;
            return result;
        }
    }

    /**
     * Wrapper to restrict use of methods for injected JMSContext (JMS 2.0 spec, §12.4.5)
     * and lazily create the real JMSContext depending on the transaction status.
     */
    private class JMSContextWrapper implements JMSContext {

        private final JMSInfo info;
        private JMSContext delegate;
        private boolean inTransaction = false;

        JMSContextWrapper(JMSInfo info) {
            this.info = info;
        }

        private JMSContext create(JMSInfo info, boolean inTx) {
            inTransaction = inTx;
            ConnectionFactory cf = (ConnectionFactory) lookup(info.connectionFactoryLookup);
            if (inTransaction) {
                XAJMSContext xaContext = ((XAConnectionFactory) cf).createXAContext(info.userName, info.password);
                return xaContext.getContext();
            } else {
                return cf.createContext(info.userName, info.password, info.ackMode);
            }
        }

        private void internalClose() {
            if (delegate != null && !inTransaction) {
                delegate.close();
                delegate = null;
            }
        }

        /**
         * create the underlying JMSContext or return it if there is already one create.
         */
        private synchronized JMSContext getDelegate() {
            TransactionSynchronizationRegistry txSyncRegistry = (TransactionSynchronizationRegistry) lookup(TRANSACTION_SYNCHRONIZATION_REGISTRY_LOOKUP);
            boolean inTx = txSyncRegistry.getTransactionStatus() == Status.STATUS_ACTIVE;
            if (inTx) {
                Object resource = txSyncRegistry.getResource(TRANSACTION_KEY);
                if (resource != null) {
                    return (JMSContext) resource;
                } else {
                    final JMSContext transactedContext = create(info, inTx);
                    txSyncRegistry.putResource(TRANSACTION_KEY, resource);
                    txSyncRegistry.registerInterposedSynchronization(new Synchronization() {
                        @Override
                        public void beforeCompletion() {
                        }

                        @Override
                        public synchronized void afterCompletion(int status) {
                            transactedContext.close();
                            inTransaction = false;
                        }
                    });
                    return transactedContext;
                }
            } else {
                if (delegate == null) {
                    try {
                        delegate = create(info, inTx);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                return delegate;
            }
        }

        private Object lookup(String name) {
            Context ctx = null;
            try {
                ctx = new InitialContext();
                return ctx.lookup(name);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (ctx != null) {
                    try {
                        ctx.close();
                    } catch (NamingException e) {
                    }
                }
            }
        }

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

        @Override
        public String toString() {
            return "JMSContextWrapper{" +
                    ", delegate=" + getDelegate() +
                    ", inTransaction=" + inTransaction +
                    '}';
        }
    }
}
