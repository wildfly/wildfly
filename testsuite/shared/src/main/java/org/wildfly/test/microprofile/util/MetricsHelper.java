/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.microprofile.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.as.arquillian.container.ManagementClient;
import org.junit.Assert;

public class MetricsHelper {


    public static String getPrometheusMetrics(ManagementClient managementClient, String scope, boolean metricMustExist) throws IOException {
        return getMetrics(managementClient, null, scope, metricMustExist);
    }

    public static String getJSONMetrics(ManagementClient managementClient, String scope, boolean metricMustExist) throws IOException {
        return getMetrics(managementClient, "application/json", scope, metricMustExist);
    }

    private static String getMetrics(ManagementClient managementClient, String accept, String scope, boolean metricMustExist) throws IOException {
        String url = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/metrics";
        if (scope != null && !scope.isEmpty()) {
            url += "/" + scope;
        }

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpGet getMetrics = new HttpGet(url);
            if (accept != null) {
                getMetrics.addHeader("Accept", accept);
            }

            try (CloseableHttpResponse resp = client.execute(getMetrics)) {
                if (metricMustExist) {
                    assertEquals(200, resp.getStatusLine().getStatusCode());
                    String content = EntityUtils.toString(resp.getEntity());
                    assertNotNull(content);
                    return content;
                } else {
                    assertEquals(204, resp.getStatusLine().getStatusCode());
                    return null;
                }
            }
        }
    }

    public static double getMetricValueFromPrometheusOutput(String prometheusContent, String scope, String metricName) {
        assertNotNull(prometheusContent);

        String[] lines = prometheusContent.split("\\R");

        for (String line : lines) {
            if (line.startsWith(scope + "_" + metricName)) {
                String longStr = line.substring(line.lastIndexOf(' '));
                return Double.parseDouble(longStr.trim());
            }
        }

        Assert.fail(scope + "_" + metricName + " metric not found in " + prometheusContent);
        return -1;
    }

    public static double getMetricValueFromJSONOutput(String jsonContent, String counterName) {
        try (
                JsonReader jsonReader = Json.createReader(new StringReader(jsonContent))
        ) {
            JsonObject payload = jsonReader.readObject();
            JsonNumber count = payload.getJsonNumber(counterName);
            return count.doubleValue();
        }
    }

    /**
     * Get json subvalue of some value.
     */
    public static double getMetricSubValueFromJSONOutput(String jsonContent, String value, String counterName) {
        try (
            JsonReader jsonReader = Json.createReader(new StringReader(jsonContent))
        ) {
            JsonObject payload = (JsonObject) jsonReader.readObject().get(value);
            JsonNumber count = payload.getJsonNumber(counterName);
            return count.doubleValue();
        }
    }
}
