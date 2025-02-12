/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.dmr.ModelNode;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

public class MicroProfileHealthUtils {

    static void testHttpEndPoint(final String healthURL, final boolean mustBeUP, final String probeName) throws IOException {
        testHttpEndPoint(healthURL, mustBeUP, probeName, null);
    }

    static void testHttpEndPoint(final String healthURL, final boolean mustBeUP, final String probeName, final Integer expectedChecksCount) throws IOException {

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            CloseableHttpResponse resp = client.execute(new HttpGet(healthURL));
            String content = EntityUtils.toString(resp.getEntity());
            resp.close();

            assertEquals(content, mustBeUP ? 200 : 503, resp.getStatusLine().getStatusCode());

            try (JsonReader jsonReader = Json.createReader(new StringReader(content))) {
                JsonObject payload = jsonReader.readObject();
                String outcome = payload.getString("status");
                assertEquals(mustBeUP ? "UP" : "DOWN", outcome);

                List<JsonValue> checks = payload.getJsonArray("checks") == null ?
                        new ArrayList<JsonValue>() : payload.getJsonArray("checks");
                if (expectedChecksCount != null) {
                    assertEquals(expectedChecksCount.intValue(), checks.size());
                }
                if (probeName != null) {
                    for (JsonValue check : checks) {
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

    static void testManagementOperation(final ModelNode response, final boolean mustBeUP, final String probeName) {
        testManagementOperation(response, mustBeUP, probeName, null);
    }

    static void testManagementOperation(final ModelNode response, final boolean mustBeUP, final String probeName, final Integer expectedChecksCount) {

        final String opOutcome = response.get("outcome").asString();
        assertEquals("success", opOutcome);

        ModelNode result = response.get("result");
        assertEquals(mustBeUP ? "UP" : "DOWN", result.get("status").asString());

        List<ModelNode> checks = result.get("checks").asList();
        if (expectedChecksCount != null) {
            assertEquals(expectedChecksCount.intValue(), checks.size());
        }
        if (probeName != null) {
            for (ModelNode check : checks) {
                if (probeName.equals(check.get("name").asString())) {
                    // probe name found
                    // global outcome is driven by this probe state
                    assertEquals(mustBeUP ? "UP" : "DOWN", check.get("status").asString());
                    return;
                }
            }
            fail("Probe named " + probeName + " not found in " + result);
        }
    }
}
