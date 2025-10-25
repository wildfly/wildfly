/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.openapi;

import java.io.IOException;
import java.net.URISyntaxException;
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
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.openapi.service.TestApplication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Validates OpenAPI endpoint for a multi-module deployment.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OpenAPIMultiModuleDeploymentTestCase {
    private static final String DEPLOYMENT_NAME = OpenAPIMultiModuleDeploymentTestCase.class.getSimpleName() + ".war";
    private static final String PARENT_DEPLOYMENT_NAME = OpenAPIMultiModuleDeploymentTestCase.class.getSimpleName() + ".ear";

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        WebArchive jaxrs = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml")
                .addPackage(TestApplication.class.getPackage())
                ;
        WebArchive web = ShrinkWrap.create(WebArchive.class, "web.war")
                .setWebXML(TestApplication.class.getPackage(), "web.xml")
                ;
        return ShrinkWrap.create(EnterpriseArchive.class, PARENT_DEPLOYMENT_NAME).addAsModules(jaxrs, web);
    }

    @Test
    public void test(@ArquillianResource URL baseURL) throws IOException, URISyntaxException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(new HttpGet(baseURL.toURI().resolve("/openapi")))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                List<String> urls = validateContent(response);
                // Ensure relative urls are valid
                for (String url : urls) {
                    try (CloseableHttpResponse r = client.execute(new HttpGet(baseURL.toURI().resolve(url + "/test/echo/foo")))) {
                        Assert.assertEquals(HttpServletResponse.SC_OK, r.getStatusLine().getStatusCode());
                        Assert.assertEquals("foo", EntityUtils.toString(r.getEntity()));
                    }
                }
            }
        }
    }

    private static List<String> validateContent(HttpResponse response) throws IOException {
        Assert.assertEquals("application/yaml", response.getEntity().getContentType().getValue());

        JsonNode node = new ObjectMapper(new YAMLFactory()).reader().readTree(response.getEntity().getContent());
        JsonNode info = node.get("info");
        Assert.assertNotNull(info);
        Assert.assertEquals(DEPLOYMENT_NAME, info.get("title").asText());
        Assert.assertNull(info.findValue("description"));

        List<String> result = new LinkedList<>();
        JsonNode servers = node.get("servers");
        if (servers != null) {
            for (JsonNode server : servers) {
                result.add(server.required("url").asText());
            }
            Assert.assertFalse(result.isEmpty());
        }
        return result;
    }
}
