/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * Use Case: one ejb module, 2 web modules, not isolated.
 *
 * REST Endpoints are defined in ejb module.
 * Application class is defined in ejb module.
 *
 * @author Lin Gao
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EarApplicationRESTInEJBTestCase {

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "jaxrsapp.ear");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        jar.addPackage(HttpRequest.class.getPackage());
        jar.addClasses(EarApplicationRESTInEJBTestCase.class, HelloWorldResource.class,
                HelloWorldPathApplication.class);
        ear.addAsModule(jar);

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "web1.war");
        war1.addAsWebInfResource(WebXml.get(""), "web.xml");
        ear.addAsModule(war1);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "web2.war");
        war2.addAsWebInfResource(WebXml.get(""), "web.xml");
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
        result = performCall("/web2/hellopath/helloworld");
        assertEquals("Hello World!", result);
    }

    @Test
    public void testReadRestResources() throws Exception {
        testReadResetOnWebModule("web1");
        testReadResetOnWebModule("web2");
    }

    private void testReadResetOnWebModule(String web) throws Exception {
        ModelNode addr = new ModelNode().add("deployment", "jaxrsapp.ear").add("subdeployment", web + ".war")
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
        assertEquals("GET /" + web + "/hellopath/helloworld", restResPath.get("resource-methods").asList().get(0).asString());
    }
}
