/*
 * JBoss, Home of Professional Open Source
 * Copyright 2019, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.jaxrs.packaging.ear;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * Use Case: 2 ejb modules, 2 web modules, isolated via jboss-deployment-structure.xml.
 *
 * REST Endpoints are defined in ejb module.
 * Application is defined in web module.
 *
 * web1 depends on ejb1 only, web2 can access ejb1 and ejb2
 *
 * @author Lin Gao
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EarApplicationRESTInEJBIsolatedTestCase {

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "jaxrsapp-isolated.ear");
        ear.addAsManifestResource(new StringAsset(
                "<jboss-deployment-structure>"
                    + "    <ear-subdeployments-isolated>true</ear-subdeployments-isolated>"
                    + "    <sub-deployment name=\"web1.war\">"
                    + "        <dependencies>"
                    + "            <module name=\"deployment.jaxrsapp-isolated.ear.ejb1.jar\" />"
                    + "        </dependencies>"
                    + "    </sub-deployment>"
                    + "    <sub-deployment name=\"web2.war\">"
                    + "        <dependencies>"
                    + "            <module name=\"deployment.jaxrsapp-isolated.ear.ejb1.jar\" />"
                    + "            <module name=\"deployment.jaxrsapp-isolated.ear.ejb2.jar\" />"
                    + "        </dependencies>"
                    + "    </sub-deployment>"
                    + "</jboss-deployment-structure>"),
                "jboss-deployment-structure.xml");

        JavaArchive ejb1 = ShrinkWrap.create(JavaArchive.class, "ejb1.jar");
        ejb1.addPackage(HttpRequest.class.getPackage());
        ejb1.addClasses(EarApplicationRESTInEJBIsolatedTestCase.class, HelloWorldResource.class);
        ear.addAsModule(ejb1);

        JavaArchive ejb2 = ShrinkWrap.create(JavaArchive.class, "ejb2.jar");
        ejb2.addPackage(HttpRequest.class.getPackage());
        ejb2.addClasses(EarApplicationRESTInEJBIsolatedTestCase.class, HelloRestResource.class);
        ear.addAsModule(ejb2);

        // define a REST inside the WAR ?
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "web1.war");
        war1.addClasses(HelloWorldPathApplication.class);
        war1.addAsWebInfResource(WebXml.get(""), "web.xml");
        ear.addAsModule(war1);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "web2.war");
        war2.addAsWebInfResource(WebXml.get("<servlet-mapping>\n" +
                "        <servlet-name>jakarta.ws.rs.core.Application</servlet-name>\n" +
                "        <url-pattern>/api/*</url-pattern>\n" +
                "    </servlet-mapping>\n" +
                    "\n"), "web.xml");
        ear.addAsModule(war2);

        return ear;
    }

    @ArquillianResource
    private URL url;

    @ContainerResource
    private ManagementClient managementClient;

    private String performCall(String urlPattern) throws Exception {
        return HttpRequest.get(url + urlPattern, 10, TimeUnit.SECONDS);
    }

    @Test
    public void testJaxRs() throws Exception {
        String result = performCall("/web1/hellopath/helloworld");
        assertEquals("Hello World!", result);

        result = performCall("/web2/api/helloworld");
        assertEquals("Hello World!", result);

        result = performCall("/web2/api/hellorest");
        assertEquals("Hello Rest", result);

    }

    @Test(expected = java.io.IOException.class)
    public void testRESTNotAvailable() throws Exception {
        performCall("/web1/hellopath/hellorest");
    }

    @Test
    public void testReadRestResources() throws Exception {
        testRestReadHelloWorldResource("web1", "hellopath");
        testRestReadHelloWorldResource("web2", "api");

        testRestReadHelloRestResourceOnWeb1();
        testRestReadHelloRestResourceOnWeb2();
    }

    // HelloWorldResource is visible to web1 and web2
    private void testRestReadHelloWorldResource(String web, String appPath) throws Exception {
        ModelNode addr = new ModelNode().add("deployment", "jaxrsapp-isolated.ear").add("subdeployment", web + ".war")
                .add("subsystem", "jaxrs").add("rest-resource", HelloWorldResource.class.getName());
        ModelNode operation = new ModelNode();
        operation.get(OP).set("read-resource");
        operation.get(OP_ADDR).set(addr);
        operation.get("include-runtime").set(true);

        ModelNode result = managementClient.getControllerClient().execute(operation).get("result");
        assertEquals(HelloWorldResource.class.getName(), result.get("resource-class").asString());
        ModelNode restResPath = result.get("rest-resource-paths").asList().get(0);
        assertEquals("helloworld", restResPath.get("resource-path").asString());
        assertEquals("java.lang.String " + HelloWorldResource.class.getName() + ".getMessage()",
                restResPath.get("java-method").asString());
        assertEquals("GET /" + web + "/" + appPath + "/helloworld", restResPath.get("resource-methods").asList().get(0).asString());

    }

    // HelloRestResource is not visible to web1
    private void testRestReadHelloRestResourceOnWeb1() throws Exception {
        ModelNode addr = new ModelNode().add("deployment", "jaxrsapp-isolated.ear").add("subdeployment", "web1.war")
                .add("subsystem", "jaxrs").add("rest-resource", HelloRestResource.class.getName());
        ModelNode operation = new ModelNode();
        operation.get(OP).set("read-resource");
        operation.get(OP_ADDR).set(addr);
        operation.get("include-runtime").set(true);
        ModelNode result = managementClient.getControllerClient().execute(operation);
        assertEquals("failed", result.get("outcome").asString());
        String failureDescription = result.get("failure-description").asString();
        assertTrue(failureDescription.contains("WFLYCTL0216"));
        assertTrue(failureDescription.contains("org.jboss.as.test.integration.jaxrs.packaging.ear.HelloRestResource"));
        assertTrue(failureDescription.contains("not found"));
    }

    // HelloRestResource is visible to web2
    private void testRestReadHelloRestResourceOnWeb2() throws Exception {
        ModelNode addr = new ModelNode().add("deployment", "jaxrsapp-isolated.ear").add("subdeployment", "web2.war")
                .add("subsystem", "jaxrs").add("rest-resource", HelloRestResource.class.getName());
        ModelNode operation = new ModelNode();
        operation.get(OP).set("read-resource");
        operation.get(OP_ADDR).set(addr);
        operation.get("include-runtime").set(true);
        ModelNode result = managementClient.getControllerClient().execute(operation).get("result");
        assertEquals(HelloRestResource.class.getName(), result.get("resource-class").asString());
        ModelNode restResPath = result.get("rest-resource-paths").asList().get(0);
        assertEquals("hellorest", restResPath.get("resource-path").asString());
        assertEquals("java.lang.String " + HelloRestResource.class.getName() + ".getMessage()",
                restResPath.get("java-method").asString());
        assertEquals("GET /web2/api/hellorest", restResPath.get("resource-methods").asList().get(0).asString());

    }

}
