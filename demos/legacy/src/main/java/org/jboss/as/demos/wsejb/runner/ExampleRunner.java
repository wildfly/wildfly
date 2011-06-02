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
package org.jboss.as.demos.wsejb.runner;

import static org.jboss.as.protocol.StreamUtils.safeClose;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

import org.jboss.as.demos.DeploymentUtils;
import org.jboss.as.demos.wsejb.archive.EndpointImpl;

/**
 * @author <a href="alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class ExampleRunner {

    public static void main(String[] args) throws Exception {
        DeploymentUtils utils = null;
        try {
            utils = new DeploymentUtils();
            utils.addDeployment("wsejb-example.jar", true, EndpointImpl.class.getPackage());
            utils.deploy();
            testWSDL();
            testSOAPCall();
        } finally {
            utils.undeploy();
            safeClose(utils);
        }
    }

    private static void testWSDL() throws Exception {
        URLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL("http://localhost:8080/wsejb-example/EndpointService/EndpointImpl?wsdl");
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
    }

    private static void testSOAPCall() throws Exception {
        URLConnection conn = null;
        InputStream in = null;
        OutputStreamWriter osw = null;
        final String message = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:arc=\"http://archive.ws.demos.as.jboss.org/\">"
                + "  <soapenv:Header/>"
                + "  <soapenv:Body>"
                + "    <arc:echo>"
                + "      <arg0>Foo</arg0>"
                + "    </arc:echo>"
                + "  </soapenv:Body>"
                + "</soapenv:Envelope>";
        try {
            URL url = new URL("http://localhost:8080/wsejb-example/EndpointService/EndpointImpl");
            System.out.println("Reading response from " + url + ":");
            conn = url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            osw = new OutputStreamWriter(conn.getOutputStream());
            osw.write(message);
            osw.flush();
            in = new BufferedInputStream(conn.getInputStream());
            int i = in.read();
            while (i != -1) {
                System.out.print((char) i);
                i = in.read();
            }
            System.out.println("");
        } finally {
            safeClose(osw);
            safeClose(in);
        }
    }

}
