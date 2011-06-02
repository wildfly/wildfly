/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.demos.ws.runner;

import static org.jboss.as.protocol.StreamUtils.safeClose;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.spi.Provider;

import org.jboss.as.demos.DeploymentUtils;
import org.jboss.as.demos.ws.archive.Endpoint;
import org.jboss.as.demos.ws.archive.EndpointImpl;

/**
 *
 * @author <a href="alessio.soldano@jboss.com">Alessio Soldano</a>
 * @version $Revision: 1.1 $
 */
public class ExampleRunner {

    public static void main(String[] args) throws Exception {
        DeploymentUtils utils = null;
        try {
            utils = new DeploymentUtils();
            utils.addWarDeployment("ws-example.war", true, EndpointImpl.class.getPackage());
            utils.deploy();
            testWebServiceRef();
            testAccess();
        } finally {
            utils.undeploy();
            safeClose(utils);
        }
    }

    private static void testAccess() throws Exception {
        URLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL("http://localhost:8080/ws-example/?wsdl");
            System.out.println("Reading response from " + url + ":");
            conn = url.openConnection();
            conn.setDoInput(true);
            in = new BufferedInputStream(conn.getInputStream());
            int i = in.read();
            while (i != -1) {
                System.out.print((char) i);
                i = in.read();
            }
            System.out.println("");
        } finally {
            safeClose(in);
        }
        URL wsdlURL = new URL("http://localhost:8080/ws-example?wsdl");
        QName serviceName = new QName("http://archive.ws.demos.as.jboss.org/", "EndpointService");
        System.out.println("JAXWS Client provider being used: " + Provider.provider().getClass());
        Service service = Service.create(wsdlURL, serviceName);
        Endpoint port = (Endpoint) service.getPort(Endpoint.class);
        System.out.println("Sending request 'hello' to address http://localhost:8080/ws-example");
        System.out.println("Got result : " + port.echo("hello"));
    }

    private static void testWebServiceRef() throws Exception {
        String urlPart = "servlet";
        URLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL("http://localhost:8080/ws-example/" + urlPart);
            System.out.println("Reading response from " + url + ":");
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
            System.out.println(sb.toString());
        } finally {
            safeClose(in);
        }
    }

    private static void usage(Throwable t) throws Exception {
        System.out.println("Caught " + t.toString());
        System.out.println("This is most likely due to the following:");
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
