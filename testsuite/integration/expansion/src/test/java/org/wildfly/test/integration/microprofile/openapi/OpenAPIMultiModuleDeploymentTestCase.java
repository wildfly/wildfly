/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
    void test(@ArquillianResource URL baseURL) throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(new HttpGet(baseURL.toURI().resolve("/openapi")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertEquals("application/yaml", response.getEntity().getContentType().getValue());

                JsonNode node = new ObjectMapper(new YAMLFactory()).reader().readTree(response.getEntity().getContent());
                JsonNode info = node.get("info");
                assertNotNull(info);
                assertEquals(DEPLOYMENT_NAME, info.get("title").asText());
                assertNull(info.findValue("description"));
            }
        }
    }
}
