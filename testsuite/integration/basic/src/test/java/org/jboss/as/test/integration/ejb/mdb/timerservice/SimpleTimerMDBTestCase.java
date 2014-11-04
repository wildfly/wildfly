/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.ejb.mdb.timerservice;

import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.jms.auxiliary.CreateTopicSetupTask;
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
        return war;

    }

    @Test
    public void testAnnotationTimeoutMethod() throws Exception {
        InitialContext ctx = new InitialContext();
        sendMessage();
        Assert.assertTrue(AnnotationTimerServiceMDB.awaitTimerCall());
    }

    @Test
    public void testTimedObjectTimeoutMethod() throws Exception {
        InitialContext ctx = new InitialContext();
        sendMessage();
        Assert.assertTrue(TimedObjectTimerServiceMDB.awaitTimerCall());
    }

    //the timer is created when the
    public void sendMessage() throws Exception {
        final InitialContext ctx = new InitialContext();
        final TopicConnectionFactory factory = (TopicConnectionFactory) ctx.lookup("java:/JmsXA");
        final TopicConnection connection = factory.createTopicConnection();
        connection.start();
        try {
            final TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            final Message message = session.createTextMessage("Test");
            final Destination destination = (Destination) ctx.lookup("topic/myAwesomeTopic");
            final MessageProducer producer = session.createProducer(destination);
            producer.send(message);
            producer.close();
        } finally {
            connection.close();
        }
    }


}
