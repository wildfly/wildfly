/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.containerstart;

import static java.util.concurrent.TimeUnit.SECONDS;
import static jakarta.jms.DeliveryMode.NON_PERSISTENT;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.logging.Logger;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:jboss/exported/queue/sendMessage"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "10"),
        @ActivationConfigProperty(propertyName = "useDLQ", propertyValue = "false") })
public class ReplyingMDB implements MessageListener {
    private static final Logger log = Logger.getLogger(ReplyingMDB.class);

    @Resource(lookup = "java:/ConnectionFactory")
    private ConnectionFactory factory;

    private Connection connection;
    private Session session;
    private MessageProducer sender;

    private static final int WAIT_S = TimeoutUtil.adjust(15);

    public void onMessage(Message m) {
        try {
            TextMessage message = (TextMessage) m;
            String text = message.getText();

            TextMessage replyMessage;
            if (message instanceof TextMessage) {
                if (text.equals("await") && !message.getJMSRedelivered()) {
                    // we have received the first message
                    log.debugf("Message [%s, %s] contains text 'await'", message, text);
                    // synchronize with test to start with undeploy
                    HelperSingletonImpl.barrier.await(WAIT_S, SECONDS);
                    HelperSingletonImpl.barrier.reset();
                    // since HornetQ 2.4.3.Final, the MDB is *not* interrupted when it is undeployed
                    // (undeployment waits till the MDB ends with onMessage processing)
                    // waiting to get transaction timeout
                    try {
                        Thread.sleep(SECONDS.toMillis(WAIT_S));
                    } catch (InterruptedException ie) {
                        log.trace("Sleeping for transaction timeout was interrupted."
                                + "This is expected at least for JTS transaction.");
                    }
                    // synchronize with test to undeploy would be in processing
                    HelperSingletonImpl.barrier.await(WAIT_S, SECONDS);
                }
                replyMessage = session.createTextMessage("Reply: " + text);
            } else {
                replyMessage = session.createTextMessage("Unknown message");
            }
            Destination destination = message.getJMSReplyTo();
            message.setJMSDeliveryMode(NON_PERSISTENT);
            sender.send(destination, replyMessage);
            log.debugf("onMessage method [OK], msg: [%s] with id [%s]. Replying to destination [%s].",
                    text, message, destination);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    public void postConstruct() {
        log.trace(ReplyingMDB.class.getSimpleName() + " was created");
        try {
            connection = factory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            sender = session.createProducer(null);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void preDestroy() {
        log.trace("Destroying MDB " + ReplyingMDB.class.getSimpleName());
        try {
            if (connection != null)
                connection.close();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
