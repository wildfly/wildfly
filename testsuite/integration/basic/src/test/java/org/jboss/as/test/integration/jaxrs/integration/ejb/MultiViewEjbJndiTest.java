/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.integration.ejb;

import java.net.URI;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jaxrs.integration.ejb.resource.FarewellBean;
import org.jboss.as.test.integration.jaxrs.integration.ejb.resource.GreetBean;
import org.jboss.as.test.integration.jaxrs.integration.ejb.resource.MultiViewBean;
import org.jboss.as.test.integration.jaxrs.integration.ejb.resource.SimpleBean;
import org.jboss.as.test.integration.jaxrs.integration.ejb.resource.TestApplication;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
public class MultiViewEjbJndiTest {

    @ArquillianResource
    private URI baseUri;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        final String deploymentXml = """
                <jboss-deployment-structure>
                    <deployment>
                        <exclude-subsystems>
                            <subsystem name="weld" />
                        </exclude-subsystems>
                    </deployment>
                </jboss-deployment-structure>
                """;
        return ShrinkWrap.create(WebArchive.class, MultiViewEjbJndiTest.class.getSimpleName() + ".war")
                .addClasses(
                        FarewellBean.class,
                        GreetBean.class,
                        SimpleBean.class,
                        MultiViewBean.class,
                        TestApplication.class)
                .addAsWebInfResource(new StringAsset(deploymentXml), "jboss-deployment-structure.xml");
    }

    @Test
    public void checkGreet() {
        try (Client client = ClientBuilder.newClient()) {
            try (Response response = client.target(baseUri).path("test/multiview/greet").request().get()) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals(String.format("Hello, %s!", MultiViewBean.class.getName()), response.readEntity(String.class));
            }
        }
    }

    @Test
    public void checkFarewell() {
        try (Client client = ClientBuilder.newClient()) {
            try (Response response = client.target(baseUri).path("test/multiview/farewell").request().get()) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals(String.format("Goodbye, %s!", MultiViewBean.class.getName()), response.readEntity(String.class));
            }
        }
    }
}
