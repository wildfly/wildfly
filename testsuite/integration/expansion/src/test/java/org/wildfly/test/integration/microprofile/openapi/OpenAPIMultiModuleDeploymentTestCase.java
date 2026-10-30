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
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.test.integration.microprofile.openapi.service.TestApplication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Validates OpenAPI endpoint for a multi-module deployment.
 * @author Paul Ferraro
 */
@ExtendWith(ArquillianExtension.class)
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
                Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assertions.assertEquals("application/yaml", response.getEntity().getContentType().getValue());

                JsonNode node = new ObjectMapper(new YAMLFactory()).reader().readTree(response.getEntity().getContent());
                JsonNode info = node.get("info");
                Assertions.assertNotNull(info);
                Assertions.assertEquals(DEPLOYMENT_NAME, info.get("title").asText());
                Assertions.assertNull(info.findValue("description"));
            }
        }
    }
}
