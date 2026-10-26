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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test for the integration of Jakarta RESTful Web Services and Jakarta Bean Validation.
 *
 * @author Stuart Douglas
 * @author Gunnar Morling
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class BeanValidationIntegrationTestCase {

    @ApplicationPath("/myjaxrs")
    public static class TestApplication extends Application {
    }

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "jaxrsnoap.war")
            .addPackage(HttpRequest.class.getPackage())
            .addClasses(
                BeanValidationIntegrationTestCase.class,
                ValidatorModel.class,
                ValidatorResource.class,
                AnotherValidatorResource.class,
                YetAnotherValidatorResource.class
            );
    }

    @ArquillianResource
    private URL url;

    @Test
    public void testValidRequest() throws Exception {
        DefaultHttpClient client = new DefaultHttpClient(new PoolingClientConnectionManager());

        HttpGet get = new HttpGet(url + "myjaxrs/validate/5");
        HttpResponse result = client.execute(get);

        Assertions.assertEquals(200, result.getStatusLine().getStatusCode());
        Assertions.assertEquals("ValidatorModel{id=5}", EntityUtils.toString(result.getEntity()));
    }

    @Test
    public void testInvalidRequest() throws Exception {
        DefaultHttpClient client = new DefaultHttpClient(new PoolingClientConnectionManager());

        HttpGet get = new HttpGet(url + "myjaxrs/validate/3");
        HttpResponse result = client.execute(get);

        Assertions.assertEquals(400, result.getStatusLine().getStatusCode(), "Parameter constraint violated");
    }

    @Test
    public void testInvalidRequestCausingPropertyConstraintViolation() throws Exception {
        DefaultHttpClient client = new DefaultHttpClient(new PoolingClientConnectionManager());

        HttpGet get = new HttpGet(url + "myjaxrs/another-validate/3");
        HttpResponse result = client.execute(get);

        Assertions.assertEquals(400, result.getStatusLine().getStatusCode(), "Property constraint violated");
    }

    @Test
    public void testInvalidRequestsAreAcceptedDependingOnValidationConfiguration() throws Exception {
        DefaultHttpClient client = new DefaultHttpClient(new PoolingClientConnectionManager());

        HttpGet get = new HttpGet(url + "myjaxrs/yet-another-validate/3/disabled");
        HttpResponse result = client.execute(get);

        Assertions.assertEquals(200, result.getStatusLine().getStatusCode(), "No constraint violated");
        EntityUtils.consume(result.getEntity());

        get = new HttpGet(url + "myjaxrs/yet-another-validate/3/enabled");
        result = client.execute(get);

        Assertions.assertEquals(400, result.getStatusLine().getStatusCode(), "Parameter constraint violated");
    }

    @Test
    public void testInvalidResponse() throws Exception {
        DefaultHttpClient client = new DefaultHttpClient(new PoolingClientConnectionManager());

        HttpGet get = new HttpGet(url + "myjaxrs/validate/11");
        HttpResponse result = client.execute(get);

        Assertions.assertEquals(500, result.getStatusLine().getStatusCode(), "Return value constraint violated");
    }
}
