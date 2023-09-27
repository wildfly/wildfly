/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.manual.microprofile.health;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.as.arquillian.container.ManagementClient;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import java.io.IOException;
import java.io.StringReader;
import java.net.ConnectException;

import static org.eclipse.microprofile.health.HealthCheckResponse.Status.DOWN;
import static org.eclipse.microprofile.health.HealthCheckResponse.Status.UP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="http://xstefank.io/">Martin Stefanko</a> (c) 2021 Red Hat inc.
 */
public class MicroProfileHealthDefaultEmptyReadinessHTTPEndpointTestCase extends MicroProfileHealthDefaultEmptyReadinessTestBase {

    void checkGlobalOutcome(ManagementClient managementClient, String operation, boolean mustBeUP, String probeName) throws IOException, InvalidHealthResponseException {

        assertEquals("check-ready", operation);
        final String httpEndpoint = "/health/ready";

        final String healthURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + httpEndpoint;

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse resp = client.execute(new HttpGet(healthURL))) {

            final int statusCode = resp.getStatusLine().getStatusCode();

            // ignore 404 during the server boot
            if (404 == statusCode) {
                throw new ConnectException("System boot in process");
            }

            if (mustBeUP) {
                if (200 != statusCode) {
                    throw new InvalidHealthResponseException(UP, "HTTP status code " + statusCode);
                }
            } else {
                if (503 != statusCode) {
                    throw new InvalidHealthResponseException(DOWN, "HTTP status code " + statusCode);
                }
            }

            String content = EntityUtils.toString(resp.getEntity());
            System.out.println("Health response content: " + content);

            try (
                    JsonReader jsonReader = Json.createReader(new StringReader(content))
            ) {
                JsonObject payload = jsonReader.readObject();
                String outcome = payload.getString("status");
                assertEquals(mustBeUP ? "UP": "DOWN", outcome);

                if (probeName != null) {
                    for (JsonValue check : payload.getJsonArray("checks")) {
                        if (probeName.equals(check.asJsonObject().getString("name"))) {
                            // probe name found
                            assertEquals(mustBeUP ? "UP" : "DOWN", check.asJsonObject().getString("status"));
                            return;
                        }
                    }
                    fail("Probe named " + probeName + " not found in " + content);
                }
            }
        }
    }
}
