/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.openapi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
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
import org.wildfly.test.integration.microprofile.openapi.service.TestApplication;

/**
 * Validates usage of "mp.openapi.extensions.disable" configuration property.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OpenAPIDisabledTestCase {

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "openapi-disabled.war")
                .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml")
                .addPackage(TestApplication.class.getPackage())
                .addAsManifestResource(new StringAsset("mp.openapi.extensions.enabled=false"), "microprofile-config.properties")
                ;
    }

    @Test
    public void test(@ArquillianResource URL baseURL) throws IOException, URISyntaxException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(new HttpGet(baseURL.toURI().resolve("/openapi")))) {
                Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
            }
        }
    }
}
