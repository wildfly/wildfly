/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.serviceref;

import java.io.BufferedReader;
import java.io.FilePermission;
import java.io.InputStreamReader;
import java.net.NetPermission;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLPermission;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.PropertyPermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.as.test.shared.PropertiesValueResolver;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

/**
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ServiceRefSevletTestCase {

    @Deployment(name = "main", testable = false)
    public static JavaArchive mainDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ws-serviceref-example.jar")
                .addClasses(EJB3Bean.class, EndpointInterface.class);
        return jar;
    }

    @Deployment(name = "servletClient", testable = false)
    public static WebArchive clientDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ws-serviceref-example-servlet-client.war")
                .addClasses(EndpointInterface.class, EndpointService.class, ServletClient.class)
                .addAsWebInfResource(ServiceRefSevletTestCase.class.getPackage(), "web.xml", "web.xml")
                .addAsWebInfResource(ServiceRefSevletTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");

        String wsdl = FileUtils.readFile(ServiceRefSevletTestCase.class, "TestService.wsdl");
        final Properties properties = new Properties();
        properties.putAll(System.getProperties());
        final String node0 = NetworkUtils.formatPossibleIpv6Address((String) properties.get("node0"));
        if (properties.containsKey("node0")) {
            properties.put("node0", node0);
        }
        war.addAsWebInfResource(new StringAsset(PropertiesValueResolver.replaceProperties(wsdl, properties)), "wsdl/TestService.wsdl");
        // all the following permissions are needed because EndpointService directly extends jakarta.xml.ws.Service class
        // and CXF guys are not willing to add more privileged blocks into their code, thus deployments need to have
        // the following permissions (note that the wsdl.properties permission is needed by wsdl4j)
        war.addAsManifestResource(createPermissionsXmlAsset(
                new FilePermission("<<ALL FILES>>", "read"),
                new PropertyPermission("user.dir", "read"),
                new RuntimePermission("getClassLoader"),
                new RuntimePermission("org.apache.cxf.permission", "resolveUri"),
                new RuntimePermission("createClassLoader"),
                new RuntimePermission("accessDeclaredMembers"),
                //Required by the ProxySelector in the new HttpClientHTTPConduit
                //This can be removed after the https://issues.apache.org/jira/browse/CXF-8933 is included
                new NetPermission("getProxySelector"),
                //Removed this after https://issues.apache.org/jira/browse/CXF-8935 is fxied
                new URLPermission("http:*", "POST:*"),
                new SocketPermission(node0 + ":8080", "connect,resolve")), "jboss-permissions.xml");
        return war;
    }

    @ArquillianResource
    @OperateOnDeployment("servletClient")
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
        try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            return br.readLine();
        }
    }
}
