/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.lra.coordinator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.openapi.service.EchoResource;
import org.wildfly.test.integration.microprofile.openapi.service.TestApplication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Validates that the LRA coordinator exposes its OpenAPI model through the /openapi endpoint
 * when both the LRA coordinator and OpenAPI subsystems are enabled.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(EnableLRAOpenAPISetupTask.class)
public class LRACoordinatorOpenAPITestCase {

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "lra-openapi-test.war")
                .addClasses(TestApplication.class, EchoResource.class)
                .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml").addAsManifestResource(
                        new StringAsset("mp.openapi.extensions.path=/openapi"), "microprofile-config.properties");
    }

    @ArquillianResource
    private URL baseURL;

    @Test
    public void testLRACoordinatorOpenAPI() throws IOException, URISyntaxException, InterruptedException {
        // The LRA coordinator OpenAPI service starts in PASSIVE mode, so it may not have
        // registered its model by the time the deployment completes. Poll until the paths appear.
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);
        List<String> pathList = null;

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            while (System.currentTimeMillis() < deadline) {
                try (CloseableHttpResponse response = client.execute(
                        new HttpGet(baseURL.toURI().resolve("/openapi?format=JSON")))) {
                    Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());

                    JsonNode root = new ObjectMapper().reader().readTree(response.getEntity().getContent());
                    JsonNode paths = root.get("paths");
                    if (paths != null) {
                        pathList = new ArrayList<>();
                        Iterator<String> pathNames = paths.fieldNames();
                        while (pathNames.hasNext()) {
                            pathList.add(pathNames.next());
                        }
                        if (pathList.stream().anyMatch(p -> p.startsWith("/lra-coordinator/"))) {
                            break;
                        }
                    }
                }
                Thread.sleep(200);
            }
        }

        Assert.assertNotNull("OpenAPI document should contain paths", pathList);

        Assert.assertTrue(
                "OpenAPI document should contain LRA coordinator paths (starting with /lra-coordinator/), but got: " + pathList,
                pathList.stream().anyMatch(p -> p.startsWith("/lra-coordinator/")));

        Assert.assertTrue(
                "OpenAPI should contain LRA start endpoint (/lra-coordinator/lra-coordinator/start), but got: " + pathList,
                pathList.contains("/lra-coordinator/lra-coordinator/start"));

        Assert.assertTrue(
                "OpenAPI should also contain the deployed application's echo endpoint, but got: " + pathList,
                pathList.stream().anyMatch(p -> p.contains("/echo/")));
    }
}
