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
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for Jakarta RESTful Web Services taking the global Jakarta Bean Validation configuration into account (META-INF/validation.xml).
 *
 * @author Gunnar Morling
 */
@RunWith(Arquillian.class)
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

        Assert.assertEquals("No constraint violated", 200, result.getStatusLine().getStatusCode());
        EntityUtils.consume(result.getEntity());

        get = new HttpGet(url + "myjaxrs/globally-configured-validate/3/enabled");
        result = client.execute(get);
        Assert.assertEquals("Parameter constraint violated", 400, result.getStatusLine().getStatusCode());
    }
}
