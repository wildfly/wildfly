/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.mdb.timerservice;

import jakarta.jms.Destination;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TopicConnection;
import jakarta.jms.TopicConnectionFactory;
import jakarta.jms.TopicSession;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.jms.auxiliary.CreateTopicSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that an @Timout method is called when a timer is created programatically.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@ServerSetup(CreateTopicSetupTask.class)
public class SimpleTimerMDBTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "testTimerServiceSimple.war");
        war.addPackage(SimpleTimerMDBTestCase.class.getPackage());
        war.addClass(CreateTopicSetupTask.class);
        war.addClass(TimeoutUtil.class);
        return war;

    }

    @Test
    public void testTimeoutMethods() throws Exception {
        sendMessage();
        Assert.assertTrue(AnnotationTimerServiceMDB.awaitTimerCall(30000));
        Assert.assertTrue(TimedObjectTimerServiceMDB.awaitTimerCall(30000));
    }

    //the timer is created when the
    public void sendMessage() throws Exception {
        final InitialContext ctx = new InitialContext();
        final TopicConnectionFactory factory = (TopicConnectionFactory) ctx.lookup("java:/JmsXA");
        try (TopicConnection connection = factory.createTopicConnection()){
            connection.start();
            final TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            final Message message = session.createTextMessage("Test");
            final Destination destination = (Destination) ctx.lookup("topic/myAwesomeTopic");
            try (MessageProducer producer = session.createProducer(destination)) {
                producer.send(message);
            }
        }
        ctx.close();
    }
}
