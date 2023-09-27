/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;

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

    static void testHttpEndPoint(String healthURL, boolean mustBeUP, String probeName) throws IOException {

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            CloseableHttpResponse resp = client.execute(new HttpGet(healthURL));
            String content = EntityUtils.toString(resp.getEntity());
            resp.close();

            assertEquals(content, mustBeUP ? 200 : 503, resp.getStatusLine().getStatusCode());


            try (
                    JsonReader jsonReader = Json.createReader(new StringReader(content))
            ) {
                JsonObject payload = jsonReader.readObject();
                String outcome = payload.getString("status");
                assertEquals(mustBeUP ? "UP" : "DOWN", outcome);

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

    static void testManagementOperation(ModelNode response, boolean mustBeUP, String probeName) {

        final String opOutcome = response.get("outcome").asString();
        assertEquals("success", opOutcome);

        ModelNode result = response.get("result");
        assertEquals(mustBeUP ? "UP" : "DOWN", result.get("status").asString());

        if (probeName != null) {
            for (ModelNode check : result.get("checks").asList()) {
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
