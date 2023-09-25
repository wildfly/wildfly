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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for the integration of Jakarta RESTful Web Services and Jakarta Bean Validation.
 *
 * @author Stuart Douglas
 * @author Gunnar Morling
 */
@RunWith(Arquillian.class)
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

        Assert.assertEquals(200, result.getStatusLine().getStatusCode());
        Assert.assertEquals("ValidatorModel{id=5}", EntityUtils.toString(result.getEntity()));
    }

    @Test
    public void testInvalidRequest() throws Exception {
        DefaultHttpClient client = new DefaultHttpClient(new PoolingClientConnectionManager());

        HttpGet get = new HttpGet(url + "myjaxrs/validate/3");
        HttpResponse result = client.execute(get);

        Assert.assertEquals("Parameter constraint violated", 400, result.getStatusLine().getStatusCode());
    }

    @Test
    public void testInvalidRequestCausingPropertyConstraintViolation() throws Exception {
        DefaultHttpClient client = new DefaultHttpClient(new PoolingClientConnectionManager());

        HttpGet get = new HttpGet(url + "myjaxrs/another-validate/3");
        HttpResponse result = client.execute(get);

        Assert.assertEquals("Property constraint violated", 400, result.getStatusLine().getStatusCode());
    }

    @Test
    public void testInvalidRequestsAreAcceptedDependingOnValidationConfiguration() throws Exception {
        DefaultHttpClient client = new DefaultHttpClient(new PoolingClientConnectionManager());

        HttpGet get = new HttpGet(url + "myjaxrs/yet-another-validate/3/disabled");
        HttpResponse result = client.execute(get);

        Assert.assertEquals("No constraint violated", 200, result.getStatusLine().getStatusCode());
        EntityUtils.consume(result.getEntity());

        get = new HttpGet(url + "myjaxrs/yet-another-validate/3/enabled");
        result = client.execute(get);

        Assert.assertEquals("Parameter constraint violated", 400, result.getStatusLine().getStatusCode());
    }

    @Test
    public void testInvalidResponse() throws Exception {
        DefaultHttpClient client = new DefaultHttpClient(new PoolingClientConnectionManager());

        HttpGet get = new HttpGet(url + "myjaxrs/validate/11");
        HttpResponse result = client.execute(get);

        Assert.assertEquals("Return value constraint violated", 500, result.getStatusLine().getStatusCode());
    }
}
