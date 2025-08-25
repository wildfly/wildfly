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
import org.jboss.as.test.integration.jaxrs.integration.ejb.resource.GreetBean;
import org.jboss.as.test.integration.jaxrs.integration.ejb.resource.GreetClientView;
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
public class SingleViewEjbJndiTest {

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
        return ShrinkWrap.create(WebArchive.class, SingleViewEjbJndiTest.class.getSimpleName() + ".war")
                .addClasses(
                        GreetBean.class,
                        GreetClientView.class,
                        TestApplication.class)
                .addAsWebInfResource(new StringAsset(deploymentXml), "jboss-deployment-structure.xml");
    }

    @Test
    public void checkGreet() {
        try (Client client = ClientBuilder.newClient()) {
            try (Response response = client.target(baseUri).path("test/greet/greet").request().get()) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals(String.format("Hello, %s!", GreetBean.class.getName()), response.readEntity(String.class));
            }
        }
    }
}
