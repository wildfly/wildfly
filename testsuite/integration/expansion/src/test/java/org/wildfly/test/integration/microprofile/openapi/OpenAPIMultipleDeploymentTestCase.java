/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.openapi;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.ManagementServerSetupTask;
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
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Validates that multiple REST applications from different deployments can publish to the same OpenAPI endpoint.
 * @author Paul Ferraro
 */
@ServerSetup(OpenAPIMultipleDeploymentTestCase.ConfigServerSetupTask.class)
@RunWith(Arquillian.class)
@RunAsClient
public class OpenAPIMultipleDeploymentTestCase {
    private static final String DEPLOYMENT1_NAME = "deployment-1";
    private static final String DEPLOYMENT2_NAME = "deployment-2";

    @Deployment(name = DEPLOYMENT1_NAME, testable = false)
    public static Archive<?> deployment1() {
        return deployment(DEPLOYMENT1_NAME);
    }

    @Deployment(name = DEPLOYMENT2_NAME, testable = false)
    public static Archive<?> deployment2() {
        return deployment(DEPLOYMENT2_NAME);
    }

    private static Archive<?> deployment(String deploymentName) {
        return ShrinkWrap.create(WebArchive.class, deploymentName + ".war")
                .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml")
                .addPackage(TestApplication.class.getPackage())
                .setWebXML(TestApplication.class.getPackage(), "web.xml")
                ;
    }

    @Test
    public void test(@ArquillianResource @OperateOnDeployment(DEPLOYMENT1_NAME) URL baseURL) throws IOException, URISyntaxException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(baseURL.toURI().resolve("/openapi")).GET().build(), BodyHandlers.ofString(StandardCharsets.UTF_8));
        Assert.assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
        Assert.assertEquals("application/yaml", response.headers().firstValue("Content-Type").orElse(null));
        List<String> urls = validateContent(response.body());
        // Ensure relative urls are valid
        for (String url : urls) {
            response = client.send(HttpRequest.newBuilder(baseURL.toURI().resolve(url + "/test/echo/foo")).GET().build(), BodyHandlers.ofString(StandardCharsets.UTF_8));
            Assert.assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
            Assert.assertEquals("foo", response.body());
        }
    }

    private static List<String> validateContent(String body) throws IOException {
        JsonNode node = new ObjectMapper(new YAMLFactory()).reader().readTree(body);
        JsonNode info = node.get("info");
        Assert.assertNotNull(body, info);
        // Info will originate from deployment model, as this is the same per deployment
        Assert.assertEquals(body, "Test application", info.get("title").asText());
        Assert.assertEquals(body, "This is my test application description", info.get("description").asText());

        JsonNode externalDocumentation = node.get("externalDocs");
        Assert.assertNotNull(body, externalDocumentation);
        // External documentation will originate from system property, as this is not specified deployment models
        Assert.assertEquals(body, "wildfly.org", externalDocumentation.get("url").asText());

        JsonNode servers = node.required("servers");
        List<String> result = new LinkedList<>();
        for (JsonNode server : servers) {
            result.add(server.required("url").asText());
        }
        Assert.assertFalse(result.isEmpty());
        return result;
    }

    public static class ConfigServerSetupTask extends ManagementServerSetupTask {

        public ConfigServerSetupTask() {
            super(createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.externalDocs.url:add(value=wildfly.org)")
                            .build())
                    .tearDownScript(createScriptBuilder()
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.externalDocs.url:remove")
                            .build())
                    .build());
        }
    }
}
