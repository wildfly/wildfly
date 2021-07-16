/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.observability.opentelemetry;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.observability.opentelemetry.application.Service1;
import org.wildfly.test.integration.observability.opentelemetry.application.Service2;
import org.wildfly.test.integration.observability.opentelemetry.application.TestApplication;

@RunWith(Arquillian.class)
@RunAsClient
public class MultiserviceTestCase {
    public static final String DEPLOYMENTA = "deploymenta";
    public static final String DEPLOYMENTB = "deploymentb";

    private static final String WEB_XML
            = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<web-app xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://java.sun.com/xml/ns/javaee\"\n"
            + "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n"
            + "         metadata-complete=\"false\" version=\"3.0\">\n"
            + "    <servlet-mapping>\n"
            + "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n"
            + "        <url-pattern>/*</url-pattern>\n"
            + "    </servlet-mapping>"
            + "</web-app>";

    public static final String WEB_XML2 =
            "<!DOCTYPE web-app PUBLIC\n" +
                    " \"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\"\n" +
                    " \"http://java.sun.com/dtd/web-app_2_3.dtd\" >\n" +
                    "\n" +
                    "<web-app>\n" +
                    "    <servlet-mapping>\n" +
                    "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n" +
                    "        <url-pattern>/*</url-pattern>\n" +
                    "    </servlet-mapping>\n" +
                    "</web-app>";

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = DEPLOYMENTA, managed = false, testable = false)
    public static Archive<?> deploy1() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENTA + ".war")
                .addClass(TestApplication.class)
                .addClass(Service1.class)
                .addAsWebInfResource(new StringAsset(WEB_XML), "web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Deployment(name = DEPLOYMENTB, managed = false, testable = false)
    public static Archive<?> deploy2() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENTB + ".war")
                .addClass(TestApplication.class)
                .addClass(Service2.class)
                .addAsWebInfResource(new StringAsset(WEB_XML), "web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    @RunAsClient
    public void testContextPropagation() throws InterruptedException {
        deployer.deploy(DEPLOYMENTA);
        deployer.deploy(DEPLOYMENTB);

        System.out.println("\n\n\n\n\n\n\n\n\n");
        String uri = "http://localhost:8080/" + DEPLOYMENTA; // + "/endpoint1";
        Client client = ClientBuilder.newClient();
        System.out.println(uri);
        Response response = client
                .target(uri)
                .request()
                .get();

        Assert.assertEquals(200, response.getStatus());
        String entity = response.readEntity(String.class);
        System.out.println("entity = " + entity);
        System.out.println("\n\n\n\n\n\n\n\n\n");

        deployer.undeploy(DEPLOYMENTA);
        deployer.undeploy(DEPLOYMENTB);
    }

}
