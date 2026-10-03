/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.test.integration.microprofile.openapi.service.TestApplication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Validates usage of "mp.openapi.extensions.path" configuration property.
 * @author Paul Ferraro
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class OpenAPIAltPathTestCase {
    private static final String DEPLOYMENT_NAME = OpenAPIAltPathTestCase.class.getSimpleName() + ".war";

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml")
                .addPackage(TestApplication.class.getPackage())
                .addAsManifestResource(new StringAsset("mp.openapi.extensions.path=/swagger"), "microprofile-config.properties")
                ;
    }

    @Test
    void test(@ArquillianResource URL baseURL) throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(new HttpGet(baseURL.toURI().resolve("/openapi")))) {
                assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
            }
            try (CloseableHttpResponse response = client.execute(new HttpGet(baseURL.toURI().resolve("/swagger")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                List<String> urls = validateContent(response);
                // Ensure relative urls are valid
                for (String url : urls) {
                    try (CloseableHttpResponse r = client.execute(new HttpGet(baseURL.toURI().resolve(url + "/test/echo/foo")))) {
                        assertEquals(HttpServletResponse.SC_OK, r.getStatusLine().getStatusCode());
                        assertEquals("foo", EntityUtils.toString(r.getEntity()));
                    }
                }
            }
        }
    }

    private static List<String> validateContent(HttpResponse response) throws IOException {
        assertEquals("application/yaml", response.getEntity().getContentType().getValue());

        JsonNode node = new ObjectMapper(new YAMLFactory()).reader().readTree(response.getEntity().getContent());
        JsonNode info = node.findValue("info");
        assertNotNull(info);
        assertEquals(DEPLOYMENT_NAME, info.get("title").asText());
        assertNull(info.findValue("description"));

        List<String> result = new LinkedList<>();
        JsonNode servers = node.get("servers");
        if (servers != null) {
            for (JsonNode server : servers) {
                result.add(server.required("url").asText());
            }
            assertFalse(result.isEmpty());
        }
        return result;
    }
}
