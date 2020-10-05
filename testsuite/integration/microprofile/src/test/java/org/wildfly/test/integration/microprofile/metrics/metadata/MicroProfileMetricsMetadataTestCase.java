/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.microprofile.metrics.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class MicroProfileMetricsMetadataTestCase {
    @ContainerResource
    ManagementClient managementClient;

    @Test
    public void testJsonHeader() throws Exception {
        final String endpointURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/metrics";
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpOptions request = new HttpOptions(endpointURL);
            // Due to a regression in https://github.com/smallrye/smallrye-metrics/issues/151, the media type can not specify its charset
            request.setHeader("Accept", "application/json");
            CloseableHttpResponse resp = client.execute(request);
            String content = EntityUtils.toString(resp.getEntity());
            assertEquals(content, 200, resp.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testNoJsonHeader() throws Exception {
        final String endpointURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/metrics";
        try (CloseableHttpClient client = HttpClients.createDefault()) {

            CloseableHttpResponse resp = client.execute(new HttpOptions(endpointURL));
            String content = EntityUtils.toString(resp.getEntity());
            resp.close();
            // smallrye-metrics no reports 405 Method Not Allowed when an Options is requested with out the JSON type
            assertEquals( 405, resp.getStatusLine().getStatusCode());
            assertTrue("'OPTIONS method is only allowed with application/json media type.' message is expected, but was: " + content,
                    content.contains("OPTIONS method is only allowed with application/json media type."));
        }
    }

    @Test
    public void testAllRegisteredMetrics() throws Exception {
        String content = getMetricsMetadata("/metrics");

        try (JsonReader jsonReader = Json.createReader(new StringReader(content))) {
            JsonObject payload = jsonReader.readObject();
            JsonObject base = payload.getJsonObject("base");
            JsonObject jvmUptime = base.getJsonObject("jvm.uptime");
            checkJvmUptime(jvmUptime);
        }
    }

    @Test
    public void testBaseScope() throws Exception {
        String content = getMetricsMetadata("/metrics/base");

        try (JsonReader jsonReader = Json.createReader(new StringReader(content))) {
            JsonObject base = jsonReader.readObject();
            JsonObject jvmUptime = base.getJsonObject("jvm.uptime");
            checkJvmUptime(jvmUptime);
        }
    }

    @Test
    public void testJvmUptimeMetric() throws Exception {
        String content = getMetricsMetadata("/metrics/base/jvm.uptime");

        try (JsonReader jsonReader = Json.createReader(new StringReader(content))) {
            JsonObject base = jsonReader.readObject();
            JsonObject jvmUptime = base.getJsonObject("jvm.uptime");
            checkJvmUptime(jvmUptime);
        }
    }

    private void checkJvmUptime(JsonObject jvmUptime) {
        assertEquals("gauge", jvmUptime.getString("type"));
        assertEquals("milliseconds", jvmUptime.getString("unit"));
        assertEquals("JVM Uptime", jvmUptime.getString("displayName"));
        assertEquals("Displays the uptime of the Java virtual machine", jvmUptime.getString("description"));
    }

    private String getMetricsMetadata(String metricsPath) throws IOException {
        String content;
        final String endpointURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + metricsPath;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpOptions httpOptions = new HttpOptions(endpointURL);
            httpOptions.setHeader("Accept", "application/json");

            CloseableHttpResponse resp = client.execute(httpOptions);

            assertEquals(200, resp.getStatusLine().getStatusCode());
            content = EntityUtils.toString(resp.getEntity());
            resp.close();
        }
        return content;
    }
}
