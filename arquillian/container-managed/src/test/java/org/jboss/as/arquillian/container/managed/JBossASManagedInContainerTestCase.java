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
import java.lang.management.ManagementFactory;
import java.net.URL;

import javax.management.Attribute;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.container.managed.archive.ConfigService;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * JBossASRemoteIntegrationTestCase
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
public class JBossASManagedInContainerTestCase {

    @Deployment
    public static JavaArchive createDeployment() throws Exception {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "sar-example.sar");
        archive.addPackage(ConfigService.class.getPackage());
        archive.addClass(JBossASManagedInContainerTestCase.class);
        String path = "META-INF/jboss-service.xml";
        URL resourceURL = JBossASManagedInContainerTestCase.class.getResource("/sar-example.sar/" + path);
        archive.addAsResource(new File(resourceURL.getFile()), path);
        return archive;
    }

    @Test
    public void testDeployedService() throws Exception {
        MBeanServerConnection mbeanServer = getMBeanServer();
        ObjectName objectName = new ObjectName("jboss:name=test,type=config");

        //FIXME should have some notification happening when the deployment has been installed for client
        waitForMbean(mbeanServer, objectName);

        mbeanServer.getAttribute(objectName, "IntervalSeconds");
        mbeanServer.setAttribute(objectName, new Attribute("IntervalSeconds", 2));
    }

    protected MBeanServerConnection getMBeanServer() throws Exception {
        return ManagementFactory.getPlatformMBeanServer();
    }

    protected void waitForMbean(MBeanServerConnection mbeanServer, ObjectName name) throws Exception {
    }
}
