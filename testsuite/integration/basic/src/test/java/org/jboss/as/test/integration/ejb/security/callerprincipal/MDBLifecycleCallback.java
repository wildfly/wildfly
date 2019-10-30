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

package org.jboss.as.test.integration.ejb.security.callerprincipal;


import java.security.Principal;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.naming.NamingException;

import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.logging.Logger;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
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