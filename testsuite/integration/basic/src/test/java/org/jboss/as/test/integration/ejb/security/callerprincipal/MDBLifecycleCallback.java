/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.callerprincipal;


import java.security.Principal;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJBException;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.MessageDrivenBean;
import jakarta.ejb.MessageDrivenContext;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Queue;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueConnectionFactory;
import jakarta.jms.QueueSender;
import jakarta.jms.QueueSession;
import javax.naming.NamingException;

import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.logging.Logger;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:jboss/queue/callerPrincipal")
})
@SecurityDomain("ejb3-tests")
public class MDBLifecycleCallback implements MessageDrivenBean, MessageListener {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(MDBLifecycleCallback.class);

    @Resource(mappedName = "java:/ConnectionFactory")
    private QueueConnectionFactory qFactory;

    private MessageDrivenContext msgContext;

    private ITestResultsSingleton getSingleton() throws NamingException {
        return (ITestResultsSingleton) msgContext.lookup("java:global/single/" + TestResultsSingleton.class.getSimpleName());
    }

    @PostConstruct
    public void init() throws Exception {
        ITestResultsSingleton results = this.getSingleton();
        log.trace(MDBLifecycleCallback.class.getSimpleName() + " @PostConstruct called");

        Principal princ = null;
        try {
            princ = msgContext.getCallerPrincipal();
        } catch (IllegalStateException e) {
            results.setMdb("postconstruct", "OKstart");
            return;
        }
        results.setMdb("postconstruct", "Method getCallerPrincipal was called from @PostConstruct with result: " + princ);
    }


    @Override
    public void ejbRemove() throws EJBException {
        try {
            ITestResultsSingleton results = this.getSingleton();
            log.trace(MDBLifecycleCallback.class.getSimpleName() + " @PreDestroy called");

            Principal princ = null;
            try {
                princ = msgContext.getCallerPrincipal();
            } catch (IllegalStateException e) {
                results.setMdb("predestroy", "OKstop");
                return;
            }
            results.setMdb("predestroy", "Method getCallerPrincipal was called from @PreDestroy with result: " + princ);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Resource
    @Override
    public void setMessageDrivenContext(MessageDrivenContext ctx) throws EJBException {
        this.msgContext = ctx;

    }

    public void onMessage(Message message) {
        //log.trace("onMessage received msg: " + message.toString());
        try {
            try {
                sendReply((Queue) message.getJMSReplyTo(), message.getJMSMessageID(), null);
            } catch (Exception e) {
                sendReply((Queue) message.getJMSReplyTo(), message.getJMSMessageID(), e);
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendReply(Queue destination, String messageID, Exception e) throws JMSException {
        QueueConnection conn = qFactory.createQueueConnection();
        try {
            QueueSession session = conn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
            QueueSender sender = session.createSender(destination);
            ObjectMessage message = session.createObjectMessage(e == null ? "SUCCESS" : e);
            message.setJMSCorrelationID(messageID);
            sender.send(message, DeliveryMode.NON_PERSISTENT, 4, 500);
        } finally {
            conn.close();
        }
    }
}
