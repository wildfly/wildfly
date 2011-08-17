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
package org.jboss.as.testsuite.integration.ws.tools.jbws3207;

import static org.jboss.as.protocol.old.StreamUtils.safeClose;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

import org.jboss.as.webservices.deployer.RemoteDeployer;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.deployer.Deployer;
import org.junit.Test;

/**
 * [JBWS-3207] JBossWS remote deployer support.
 *
 * @author <a href="ropalka@redhat.com">Richard Opalka</a>
 */
public class JBWS3207TestCase {

    @Test
    public void testRemoteDeployer() throws Exception {
        SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
        Deployer deployer = spiProvider.getSPI(Deployer.class);
        assertTrue(deployer instanceof RemoteDeployer);
        final String basedir = System.getProperty("basedir");
        final String testdir = basedir + File.separatorChar + "target" + File.separatorChar + "test-libs";
        File archiveFile = new File( testdir, "jbws3207.war");
        URL archiveURL = archiveFile.toURI().toURL();
        try {
            deployer.deploy(archiveURL);
            testWSDL();
            testSOAPCall();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            deployer.undeploy(archiveURL);
        }
    }

    private static void testWSDL() throws Exception {
        URLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL("http://localhost:8080/jbws3207/?wsdl");
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
        final String message = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:arc=\"http://service.jbws3207.tools.ws.integration.testsuite.as.jboss.org/\">"
                + "  <soapenv:Header/>"
                + "  <soapenv:Body>"
                + "    <arc:echoString>"
                + "      <arg0>Foo</arg0>"
                + "    </arc:echoString>" + "  </soapenv:Body>" + "</soapenv:Envelope>";
        try {
            URL url = new URL("http://localhost:8080/jbws3207");
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
