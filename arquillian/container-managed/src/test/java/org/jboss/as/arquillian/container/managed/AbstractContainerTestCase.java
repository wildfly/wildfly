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

import javax.management.Attribute;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Thomas.Diesler@jboss.com
 */
public abstract class AbstractContainerTestCase {

    @Test
    public void testDeployedService() throws Exception {
        MBeanServerConnection mbeanServer = getMBeanServer();
        ObjectName objectName = new ObjectName("jboss:name=test,type=config");

        //FIXME should have some notification happening when the deployment has been installed for client
        waitForMbean(mbeanServer, objectName);

        mbeanServer.getAttribute(objectName, "IntervalSeconds");
        mbeanServer.setAttribute(objectName, new Attribute("IntervalSeconds", 2));
    }

    abstract MBeanServerConnection getMBeanServer() throws Exception;

    void waitForMbean(MBeanServerConnection mbeanServer, ObjectName name) throws Exception {
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
