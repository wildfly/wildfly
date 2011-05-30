/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.container.managed;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.container.managed.archive.ConfigService;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.runner.RunWith;

/**
 * JBossASRemoteIntegrationTestCase
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JBossASManagedAsClientTestCase extends JBossASManagedInContainerTestCase {

    @Deployment
    public static JavaArchive createDeployment() throws Exception {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "sar-example.sar");
        archive.addPackage(ConfigService.class.getPackage());
        String path = "META-INF/jboss-service.xml";
        URL resourceURL = JBossASManagedAsClientTestCase.class.getResource("/sar-example.sar/" + path);
        archive.addAsResource(new File(resourceURL.getFile()), path);
        return archive;
    }



    @Override
    protected MBeanServerConnection getMBeanServer() throws Exception {
        int port = 1090;
        String urlString = System.getProperty("jmx.service.url", "service:jmx:rmi:///jndi/rmi://127.0.0.1:" + port + "/jmxrmi");
        try {
            JMXServiceURL serviceURL = new JMXServiceURL(urlString);
            JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceURL, null);
            return jmxConnector.getMBeanServerConnection();
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot obtain MBeanServerConnection to: " + urlString, ex);
        }

    }

    @Override
    protected void waitForMbean(MBeanServerConnection mbeanServer, ObjectName name) throws Exception {
        //FIXME remove this
        long end = System.currentTimeMillis() + 3000;
        do {
            try {
                MBeanInfo info = mbeanServer.getMBeanInfo(name);
                if (info != null) {
                    return;
                }
            } catch (Exception e) {
            }
            Thread.sleep(100);
        } while (System.currentTimeMillis() < end);
    }
}
