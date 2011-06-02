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
package org.jboss.as.demos.ds.runner;

import static org.jboss.as.protocol.StreamUtils.safeClose;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.naming.NameNotFoundException;

import org.jboss.as.demos.DeploymentUtils;
import org.jboss.as.demos.ds.mbean.Test;

/**
 * Runner for the datasource demo
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class ExampleRunner {

    public static void main(String[] args) throws Exception {
        DeploymentUtils utils = null;
        try {
            utils = new DeploymentUtils("ds-mbean.sar", true, Test.class.getPackage());

            utils.deploy();
            ObjectName objectName = new ObjectName("jboss:name=test,type=ds");

            MBeanServerConnection mbeanServer = utils.getConnection();
            System.out.println("Calling TestMBean.test() on server");
            String s = (String) mbeanServer.invoke(objectName, "test", new Object[0], new String[0]);
            System.out.println("Received reply: " + s);
        } catch (Exception e) {
            Throwable parent = e;
            while (parent != null) {
                if (parent instanceof NameNotFoundException && e.getMessage().indexOf("H2DS") > -1) {
                    usage(parent);
                    return;
                }
                parent = parent.getCause();
            }
            e.printStackTrace();
        } finally {
            if (utils != null) {
                utils.undeploy();
            }
            safeClose(utils);
        }
    }

    private static void usage(Throwable t) throws Exception {
        System.out.println("Caught " + t.toString());
        System.out.println("Please make sure your standalone.xml includes the H2DS datasource in its <profile> element.");
        System.out.println("An example configuration is as follows:\n");

        System.out.println("<subsystem xmlns=\"urn:jboss:domain:datasources:1.0\">");
        System.out.println("    <datasources>");
        System.out.println("        <datasource jndi-name=\"java:/H2DS\" enabled=\"true\" use-java-context=\"true\" pool-name=\"H2DS\">");
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
