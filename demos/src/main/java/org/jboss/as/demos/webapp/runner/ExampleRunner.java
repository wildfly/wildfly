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
package org.jboss.as.demos.webapp.runner;

import static org.jboss.as.protocol.StreamUtils.safeClose;

import java.io.BufferedInputStream;
import java.io.InputStream;
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

import org.jboss.as.demos.DeploymentUtils;
import org.jboss.as.demos.fakejndi.FakeJndi;
import org.jboss.as.demos.webapp.archive.SimpleServlet;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ExampleRunner {

    public static void main(String[] args) throws Exception {
        DeploymentUtils utils = null;
        QueueConnection conn = null;
        QueueSession session = null;
        //TODO Don't do this FakeJndi stuff once we have remote JNDI working
        try {
            utils = new DeploymentUtils("fakejndi.sar", FakeJndi.class.getPackage());
            utils.addWarDeployment("webapp-example.war", SimpleServlet.class.getPackage());
            utils.deploy();

            QueueConnectionFactory qcf = lookup(utils, "RemoteConnectionFactory", QueueConnectionFactory.class);
            Queue queue = lookup(utils, "queue/test", Queue.class);
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
            connect("simple?value=Three");
        } finally {
            utils.undeploy();
            safeClose(utils);
            conn.close();
        }
    }

    private static void connect(String urlPart) throws Exception {
        URLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL("http://localhost:8080/webapp-example/" + urlPart);
            System.out.println("Reading response from " + url + ":");
            conn = url.openConnection();
            conn.setDoInput(true);
            in = new BufferedInputStream(conn.getInputStream());
            int i = in.read();
            StringBuilder sb = new StringBuilder();
            while (i != -1) {
                sb.append((char)i);
                i = in.read();
            }
            System.out.println(sb.toString());
        } finally {
            safeClose(in);
        }
    }

    private static <T> T lookup(DeploymentUtils utils, String name, Class<T> expected) throws Exception {
        MBeanServerConnection mbeanServer = utils.getConnection();
        ObjectName objectName = new ObjectName("jboss:name=test,type=fakejndi");
        Object o = mbeanServer.invoke(objectName, "lookup", new Object[] {name}, new String[] {"java.lang.String"});
        return expected.cast(o);
    }
}
