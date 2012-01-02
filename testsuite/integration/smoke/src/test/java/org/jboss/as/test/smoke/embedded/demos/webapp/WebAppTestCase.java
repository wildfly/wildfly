/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.smoke.embedded.demos.webapp;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.demos.webapp.archive.SimpleServlet;
import org.jboss.as.test.smoke.embedded.demos.fakejndi.FakeJndi;
import org.jboss.as.test.smoke.modular.utils.ShrinkWrapUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;
import static org.jboss.as.protocol.StreamUtils.safeClose;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@RunWith(Arquillian.class)
@RunAsClient
@Ignore("[AS7-814] Fix or remove ignored smoke tests")
public class WebAppTestCase {

    @Deployment(testable = false)
    public static Archive<?> getDeployment(){
        //TODO - either deploy this separately once Arquillian (post Alpha4) supports multiple deployments
        //or wait until we have real remote jndi
        Archive<?> fakeJndi = ShrinkWrapUtils.createJavaArchive("demos/fakejndi.sar", FakeJndi.class.getPackage());

        return ShrinkWrapUtils.createWebArchive("demos/webapp-example.war", SimpleServlet.class.getPackage());
    }

    @Test
    public void testWebApp() throws Exception {
        QueueConnection conn = null;
        QueueSession session = null;
        //TODO Don't do this FakeJndi stuff once we have remote JNDI working
        try {

            QueueConnectionFactory qcf = lookup("RemoteConnectionFactory", QueueConnectionFactory.class);
            Queue queue = lookup("queue/test", Queue.class);
            conn = qcf.createQueueConnection();
            conn.start();
            session = conn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);

            // Set the async listener
            QueueReceiver recv = session.createReceiver(queue);
            recv.setMessageListener(new MessageListener() {

                @Override
                public void onMessage(Message message) {
                    TextMessage msg = (TextMessage)message;
                    try {
                        System.out.println("---->Received from queue: " + msg.getText());
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });


            connect("other?value=One");
            connect("simple?value=Two");
            connect("other?value=Three");
        } finally {
            conn.close();
        }
    }

    private void connect(String urlPart) throws Exception {
        URLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL("http://localhost:8080/webapp-example/" + urlPart);
            conn = url.openConnection();
            conn.setDoInput(true);
            try {
                in = new BufferedInputStream(conn.getInputStream());
            } catch (Exception e) {
                usage(e);
                return;
            }
            int i = in.read();
            StringBuilder sb = new StringBuilder();
            while (i != -1) {
                sb.append((char)i);
                i = in.read();
            }
        } finally {
            safeClose(in);
        }
    }

    private static <T> T lookup(String name, Class<T> expected) throws Exception {
        ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("127.0.0.1"), 9999, getCallbackHandler());
        MBeanServerConnection mbeanServer = JMXConnectorFactory.connect(new JMXServiceURL("service:jmx:remote://127.0.0.1:9999")).getMBeanServerConnection();
        ObjectName objectName = new ObjectName("jboss:name=test,type=fakejndi");
        Object o = mbeanServer.invoke(objectName, "lookup", new Object[] {name}, new String[] {"java.lang.String"});
        return expected.cast(o);
    }

    private static void usage(Throwable t) throws Exception {
        System.out.println("Caught " + t.toString());
        System.out.println("This is most likely due to the following:");
        System.out.println("Please make sure your standalone.xml includes the H2DS datasource in its <profile> element.");
        System.out.println("An example configuration is as follows:\n");

        System.out.println("<subsystem xmlns=\"urn:jboss:domain:datasources:1.0\">");
        System.out.println("    <datasources>");
        System.out.println("        <datasource jndi-name=\"java:jboss/datasources/ExampleDS\" enabled=\"true\" use-java-context=\"true\" pool-name=\"H2DS\">");
        System.out.println("            <connection-url>jdbc:h2:mem:test;DB_CLOSE_DELAY=-1</connection-url>");
        System.out.println("            <driver-class>org.h2.Driver</driver-class>");
        System.out.println("            <module>com.h2database.h2</module>");
        System.out.println("            <pool></pool>");
        System.out.println("            <security>");
        System.out.println("                <user-name>sa</user-name>");
        System.out.println("                <password>sa</password>");
        System.out.println("            </security>");
        System.out.println("            <validation></validation>");
        System.out.println("            <time-out></time-out>");
        System.out.println("            <statement></statement>");
        System.out.println("        </datasource>");
        System.out.println("    </datasources>");
        System.out.println("</subsystem>");

        System.out.println("\nIf your profile already includes other datasource configurations, just add the nested <datasource> element above next to them.");
    }

}
