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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
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

    static String path = null;

    static class ValveSetup implements ServerSetupTask {

        static final String modulename = "org.my.valve";
        static final String classname = "org.jboss.as.test.integration.web.valve.MyValve";
        static final String baseModulePath = "/../modules/org/my/valve/main";
        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            path = ValveUtil.readASPath(managementClient.getControllerClient());
            File file = new File(path);
            if (file.exists()) {
                file = new File(path + baseModulePath);
                file.mkdirs();
                file = new File(path + baseModulePath + "/valves.jar");
                if (file.exists())
                    file.delete();
                createJar(file);
                file = new File(path + baseModulePath + "/module.xml");
                if (file.exists())
                    file.delete();              
                FileWriter fstream = new FileWriter(path + baseModulePath + "/module.xml");
                BufferedWriter out = new BufferedWriter(fstream);
                out.write("<module xmlns=\"urn:jboss:module:1.1\" name=\"org.my.valve\">\n");
                out.write("    <properties>\n");
                out.write("        <property name=\"jboss.api\" value=\"private\"/>\n");
                out.write("    </properties>\n");

                out.write("    <resources>\n");
                out.write("        <resource-root path=\"valves.jar\"/>\n");
                out.write("    </resources>\n");

                out.write("    <dependencies>\n");
                out.write("        <module name=\"sun.jdk\"/>\n");
                out.write("        <module name=\"javax.servlet.api\"/>\n");
                out.write("        <module name=\"org.jboss.as.web\"/>\n");
                out.write("    </dependencies>\n");
                out.write("</module>");
                out.close();
            }
            ValveUtil.createValveModule(managementClient.getControllerClient(), "myvalve", modulename, classname);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            ValveUtil.removeValve(managementClient.getControllerClient(), "myvalve");
        }
    }

    public static void createJar(File file) {

        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "valve.jar")
                              .addClass(MyValve.class);
        
        log.info(archive.toString(true));
            
        archive.as(ZipExporter.class).exportTo(file);
    }

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
        assertTrue(response.contains("MyParam"));
    }

}
