package org.wildfly.test.integration.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.as.arquillian.container.ManagementClient;
import org.junit.Assert;

public class MetricsHelper {


    public static String getPrometheusMetrics(ManagementClient managementClient, boolean metricMustExist) throws IOException {
        String url = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/metrics";

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpGet getMetrics = new HttpGet(url);
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

    public static double getMetricValueFromPrometheusOutput(String prometheusContent, String metricName) {
        assertNotNull(prometheusContent);

        String[] lines = prometheusContent.split("\\R");

        for (String line : lines) {
            if (line.startsWith(metricName)) {
                String longStr = line.substring(line.lastIndexOf(' '));
                return Double.parseDouble(longStr.trim());
            }
        }

        Assert.fail(metricName + " metric not found in " + prometheusContent);
        return -1;
    }
}
