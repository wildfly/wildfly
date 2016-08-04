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
package org.jboss.as.test.integration.ws.serviceref;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilePermission;
import java.io.InputStreamReader;
import java.net.SocketPermission;
import java.net.URL;
import java.util.Properties;
import java.util.PropertyPermission;

import org.junit.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.as.test.shared.PropertiesValueResolver;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * Tests for WS ServiceRef from servlet to verify access for <service-ref> in nested war
 *
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ServiceRefEarTestCase {

    private static final Logger log = Logger.getLogger(ServiceRefEarTestCase.class);

    @Deployment(testable = false)
    public static Archive<?> deployment() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ws-serviceref-example.jar")
                .addClasses(EJB3Bean.class, EndpointInterface.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ws-serviceref-example-servlet-client.war")
                .addClasses(EndpointInterface.class, EndpointService.class, ServletClient.class)
                .addAsWebInfResource(ServiceRefEarTestCase.class.getPackage(), "web.xml", "web.xml")
                .addAsWebInfResource(ServiceRefEarTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");

        String wsdl = FileUtils.readFile(ServiceRefEarTestCase.class, "TestService.wsdl");
        final Properties properties = new Properties();
        properties.putAll(System.getProperties());
        final String node0 = NetworkUtils.formatPossibleIpv6Address((String) properties.get("node0"));
        if (properties.containsKey("node0")) {
            properties.put("node0", node0);
        }
        war.addAsWebInfResource(new StringAsset(PropertiesValueResolver.replaceProperties(wsdl, properties)), "wsdl/TestService.wsdl");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "ws-serviceref-example.ear")
                .addAsModule(jar)
                .addAsModule(war);
        // all the following permissions are needed because EndpointService directly extends javax.xml.ws.Service class
        // and CXF guys are not willing to add more privileged blocks into their code, thus deployments need to have
        // the following permissions (note that the wsdl.properties permission is needed by wsdl4j)
        ear.addAsManifestResource(createPermissionsXmlAsset(
                new FilePermission(System.getProperty("java.home") + File.separator + "lib" + File.separator + "wsdl.properties", "read"),
                new PropertyPermission("user.dir", "read"),
                new RuntimePermission("getClassLoader"),
                new RuntimePermission("org.apache.cxf.permission", "resolveUri"),
                new RuntimePermission("createClassLoader"),
                new RuntimePermission("accessDeclaredMembers"),
                new SocketPermission(node0 + ":8080", "connect,resolve")), "jboss-permissions.xml");

        return ear;
    }

    @ArquillianResource(ServletClient.class)
    URL baseUrl;

    @Test
    public void testServletClientEcho1() throws Exception {
        String retStr = receiveFirstLineFromUrl(new URL(baseUrl.toString() + "?echo=HelloWorld&type=echo1"));
        Assert.assertEquals("Unexpected output - " + retStr, "HelloWorld", retStr);
    }

    @Test
    public void testServletClientEcho2() throws Exception {
        String retStr = receiveFirstLineFromUrl(new URL(baseUrl.toString() + "?echo=HelloWorld&type=echo2"));
        Assert.assertEquals("Unexpected output - " + retStr, "HelloWorld", retStr);
    }

    @Test
    public void testServletClientEcho3() throws Exception {
        String retStr = receiveFirstLineFromUrl(new URL(baseUrl.toString() + "?echo=HelloWorld&type=echo3"));
        Assert.assertEquals("Unexpected output - " + retStr, "HelloWorld", retStr);
    }

    @Test
    public void testServletClientEcho4() throws Exception {
        String retStr = receiveFirstLineFromUrl(new URL(baseUrl.toString() + "?echo=HelloWorld&type=echo4"));
        Assert.assertEquals("Unexpected output - " + retStr, "HelloWorld", retStr);
    }

    @Test
    public void testServletClientEcho5() throws Exception {
        String retStr = receiveFirstLineFromUrl(new URL(baseUrl.toString() + "?echo=HelloWorld&type=echo5"));
        Assert.assertEquals("Unexpected output - " + retStr, "HelloWorld", retStr);
    }

    private String receiveFirstLineFromUrl(URL url) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        try {
            return br.readLine();
        } finally {
            br.close();
        }
    }
}
