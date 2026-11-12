/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.validator;

import java.net.URL;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test for Jakarta RESTful Web Services taking the global Jakarta Bean Validation configuration into account (META-INF/validation.xml).
 *
 * @author Gunnar Morling
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class BeanValidationConfiguredGloballyTestCase {

    @ApplicationPath("/myjaxrs")
    public static class TestApplication extends Application {
    }

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "jaxrsnoap.war")
            .addPackage(HttpRequest.class.getPackage())
            .addClasses(
                BeanValidationConfiguredGloballyTestCase.class,
                ValidatorModel.class,
                GloballyConfiguredValidatorResource.class
            )
            .addAsManifestResource(new StringAsset(
                "<validation-config version=\"1.1\" xmlns=\"http://jboss.org/xml/ns/javax/validation/configuration\">\n" +
                    "<executable-validation>\n" +
                        "<default-validated-executable-types>\n" +
                            "<executable-type>NONE</executable-type>\n" +
                        "</default-validated-executable-types>\n" +
                    "</executable-validation>\n" +
                "</validation-config>"), "validation.xml"
            );
    }

    @ArquillianResource
    private URL url;

    @Test
    public void testInvalidRequestsAreAcceptedDependingOnGlobalValidationConfiguration() throws Exception {
        DefaultHttpClient client = new DefaultHttpClient(new PoolingClientConnectionManager());

        HttpGet get = new HttpGet(url + "myjaxrs/globally-configured-validate/3/disabled");
        HttpResponse result = client.execute(get);

        Assertions.assertEquals(200, result.getStatusLine().getStatusCode(), "No constraint violated");
        EntityUtils.consume(result.getEntity());

        get = new HttpGet(url + "myjaxrs/globally-configured-validate/3/enabled");
        result = client.execute(get);
        Assertions.assertEquals(400, result.getStatusLine().getStatusCode(), "Parameter constraint violated");
    }
}
