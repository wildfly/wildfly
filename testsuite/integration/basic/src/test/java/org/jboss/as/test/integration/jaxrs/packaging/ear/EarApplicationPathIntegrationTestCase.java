/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * Tests a Jakarta RESTful Web Services deployment with an application bundled, that has no @ApplicationPath annotation.
 * <p/>
 * The container should register a servlet with the name that matches the application name
 * <p/>
 * It is the app providers responsibility to provide a mapping for the servlet
 * <p/>
 * JAX-RS 1.1 2.3.2 bullet point 3
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EarApplicationPathIntegrationTestCase {

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "jaxrsapp.ear");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        jar.addPackage(HttpRequest.class.getPackage());
        jar.addClasses(EarApplicationPathIntegrationTestCase.class, HelloWorldResource.class, HelloWorldPathApplication.class);
        ear.addAsModule(jar);

        JavaArchive jar2 = ShrinkWrap.create(JavaArchive.class, "ejb2.jar");
        jar2.addClass(SimpleEjb.class);
        ear.addAsModule(jar2);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "jaxrsapp.war");
        war.addAsWebInfResource(WebXml.get(""), "web.xml");
        ear.addAsModule(war);
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
    public void testJaxRsWithNoApplication() throws Exception {
        String result = performCall("hellopath/helloworld");
        assertEquals("Hello World!", result);
    }

    @Test
    public void testReadRestResources() throws Exception {
        ModelNode addr = new ModelNode().add("deployment", "jaxrsapp.ear").add("subdeployment", "jaxrsapp.war")
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
        assertEquals("GET /jaxrsapp/hellopath/helloworld",
                restResPath.get("resource-methods").asList().get(0).asString());
    }

}
