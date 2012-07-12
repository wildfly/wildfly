/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.smoke.jms.objectmessage;

import static java.util.concurrent.TimeUnit.SECONDS;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.naming.Context;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
@RunWith(Arquillian.class)
public class ObjectMessageTest {

    private static final Logger LOGGER = Logger.getLogger(ObjectMessageTest.class);

    @Deployment(name = "ear")
    public static EnterpriseArchive createArchive() {
        // the Document and its implementatiosn that are used both by the servlet and the MDB
        // are put in a common lib jar
        JavaArchive commons = ShrinkWrap.create(JavaArchive.class, "objectmessage-commons.jar")
                .addClass(Document.class)
                .addClass(DocumentImpl.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        LOGGER.info(commons.toString(true));

        WebArchive war = ShrinkWrap.create(WebArchive.class, "objectmessage.war")
                .addClass(ObjectMessageServlet.class)
                .addAsWebInfResource(ObjectMessageTest.class.getPackage(), "hornetq-jms.xml", "hornetq-jms.xml");
        LOGGER.info(war.toString(true));

        JavaArchive mdb = ShrinkWrap.create(JavaArchive.class, "objectmessage-ejb.jar")
                .addClass(ObjectMessageMDB.class);
        LOGGER.info(mdb.toString(true));

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "objectmessage.ear")
                .addAsLibraries(commons)
                .addAsModules(war, mdb);
        LOGGER.info(ear.toString(true));
        return ear;
    }

    @ContainerResource
    private Context context;

    @ArquillianResource
    @OperateOnDeployment("ear")
    private URL servletUrl;

    /**
     * Validation test for https://issues.jboss.org/browse/AS7-1271
     */
    @Test
    @RunAsClient
    public void objectMessageWithCustomPayload() throws Exception {
        invokeServlet();
        checkMDBHasReplied();
    }

    /**
     * If the MDB has received the ObjectMessage with the payload, it has forwarded the message
     * to the replyTo queue that we check.
     */
    private void checkMDBHasReplied() throws Exception {
        ConnectionFactory cf = (ConnectionFactory) context.lookup("jms/RemoteConnectionFactory");
        assertNotNull(cf);
        Connection connection = null;
        try {
            connection = cf.createConnection("guest", "guest");
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination replyToQueue = (Destination) context.lookup("queue/replyToQueue");
            MessageConsumer consumer = session.createConsumer(replyToQueue);
            connection.start();
            Message m = consumer.receive(SECONDS.toMillis(1));
            assertNotNull("did not get reply for MDB, check server logs", m);
            ObjectMessage message = (ObjectMessage) m;
            Object payload = message.getObject();
            assertTrue(payload instanceof Document);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private void invokeServlet() throws IOException, ExecutionException, TimeoutException {
        String res = HttpRequest.get(servletUrl.toExternalForm() + "objectmessage", 4, SECONDS);
        assertEquals("OK", res);
    }

}
