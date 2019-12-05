/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.wildfly.test.integration.microprofile.openapi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.openapi.service.TestApplication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Validates retrieval of JSON OpenAPI document via format parameter.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OpenAPIFormatTestCase {
    private static final String DEPLOYMENT_NAME = OpenAPIFormatTestCase.class.getSimpleName() + ".war";

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .addPackage(TestApplication.class.getPackage())
                .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml")
                .setWebXML(TestApplication.class.getPackage(), "web.xml")
                ;
    }

    @Test
    public void test(@ArquillianResource URL baseURL) throws IOException, URISyntaxException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpUriRequest request = new HttpGet(baseURL.toURI().resolve("/openapi?format=JSON"));
            try (CloseableHttpResponse response = client.execute(request)) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                validateContent(response);
            }
            // Ensure format parameter is still read when Accept header uses wildcard
            request.addHeader("Accept", MediaType.WILDCARD);
            try (CloseableHttpResponse response = client.execute(request)) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                validateContent(response);
            }
        }
    }

    private static void validateContent(HttpResponse response) throws IOException {
        Assert.assertEquals(MediaType.APPLICATION_JSON, response.getEntity().getContentType().getValue());

        JsonNode node = new ObjectMapper().reader().readTree(response.getEntity().getContent());
        JsonNode info = node.get("info");
        Assert.assertEquals("Test application", info.get("title").asText());
        Assert.assertEquals("This is my test application description", info.get("description").asText());
    }
}
