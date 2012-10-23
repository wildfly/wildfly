/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.web.valve;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

/**
 * This class tests a global valve.
 *
 * @author Jean-Frederic Clere
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(ValveUnitTestCase.ValveSetup.class)
public class ValveUnitTestCase {

    private static Logger log = Logger.getLogger(ValveUnitTestCase.class);

    static class ValveSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            ValveUtil.createValve(managementClient.getControllerClient(), "myvalve");
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            ValveUtil.removeValve(managementClient.getControllerClient(), "myvalve");
        }
    }

    /*
    @Deployment(name = "valve.jar", testable = false)
    public static JavaArchive createDeployment() {

        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "valve.jar")
                              .addClass(MyValve.class);
        
        System.out.println("jar is: " + archive.getName());
        log.info(archive.toString(true));
        return archive;
    }
    */
    @Deployment(name = "valve")
    public static WebArchive Hello() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "valve.war");
        war.addClasses(HttpRequest.class, HelloServlet.class);
        return war;
    }

    @Test
    @OperateOnDeployment("valve")
    public void testValve(@ArquillianResource URL url) throws Exception {
        String response = ValveUtil.hitValve(log, url);
        // AS7-5133 assertTrue(response.contains("MyParam"));
    }

}
