/*
 * Copyright 2023 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.test.integration.observability.micrometer;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import com.fasterxml.jackson.core.util.JacksonFeature;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

@RunWith(Arquillian.class)
@ServerSetup(MicrometerSetupTask.class)
public class MicrometerOtelIntegrationTestCase {

    public static final int REQUEST_COUNT = 5;
    @ArquillianResource
    private URL url;
    @Inject
    private MeterRegistry meterRegistry;

    private final Client client = ClientBuilder.newClient().register(JacksonFeature.class);

    static final String WEB_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<web-app xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n"
                    + "           xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"\n"
                    + "         xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd\" \n"
                    + "              version=\"4.0\">\n"
                    + "    <servlet-mapping>\n"
                    + "        <servlet-name>jakarta.ws.rs.core.Application</servlet-name>\n"
                    + "        <url-pattern>/*</url-pattern>\n"
                    + "    </servlet-mapping>"
                    + "</web-app>\n";

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "micrometer-test.war")
                .addClasses(ServerSetupTask.class, MetricResource.class)
                .addAsWebInfResource(new StringAsset(WEB_XML), "web.xml")
                .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml");
    }

    @BeforeClass
    public static void checkForDocker() {
        AssumeTestGroupUtil.assumeDockerAvailable();
    }

    @Test
    @InSequence(1)
    public void testInjection() {
        Assert.assertNotNull(meterRegistry);
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void makeRequests() throws URISyntaxException {
        WebTarget target = client.target(url.toURI());
        for (int i = 0; i < REQUEST_COUNT; i++) {
            target.request().get();
        }
    }

    @Test
    @InSequence(3)
    public void checkCounter() {
        Counter counter = meterRegistry.get("demo_counter").counter();
        Assert.assertEquals(counter.count(), REQUEST_COUNT, 0.0);
    }


    // Request the published metrics from the OpenTelemetry Collector via the configured Prometheus exporter and check
    // a few metrics to verify there existence
    @Test
    @RunAsClient
    @InSequence(Integer.MAX_VALUE)
    public void getMetrics() throws InterruptedException {
        WebTarget target = client.target(MicrometerSetupTask.otelCollector.getPrometheusUrl());

        int attemptCount = 0;
        boolean found = false;
        String body = "";

        // Request counts can vary. Setting high to help ensure test stability
        while (!found && attemptCount < 30) {
            // Wait to give Micrometer time to export
            Thread.sleep(1000);

            body = target.request().get().readEntity(String.class);
            found = body.contains("demo_counter");
            attemptCount++;
        }

        final String finalBody = body;
        Arrays.asList(
                "demo_counter",
                "memory_used_heap",
                "cpu_available_processors",
                "classloader_loaded_classes_count",
                "cpu_system_load_average",
                "gc_time",
                "thread_count",
                "undertow_bytes_received"
        ).forEach(n -> Assert.assertTrue("Missing metric: " + n, finalBody.contains(n)));
    }
}
