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

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup(MicrometerSetupTask.class)
public class BasicMicrometerTestCase {

    @ArquillianResource
    private URL url;

    private static final String WEB_XML =
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

    @Inject
    private MeterRegistry meterRegistry;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, BasicMicrometerTestCase.class.getSimpleName() + ".war")
                .addClasses(ServerSetupTask.class, MetricResource.class)
                .addAsWebInfResource(new StringAsset(WEB_XML), "web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    @InSequence(1)
    public void testInjection() {
        Assert.assertNotNull(meterRegistry);
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void makeRequests() throws IOException, ExecutionException, TimeoutException, InterruptedException {
        for (int i = 0; i < 5; i++) {
            HttpRequest.get(url.toString(), 10, TimeUnit.SECONDS);
        }
    }

    @Test
    @InSequence(3)
    public void checkCounter() {
        Counter counter = meterRegistry.get("demo_counter").counter();
        Assert.assertTrue(counter.count() == 5.0);
    }

    @Test
    @InSequence(4)
    public void spotCheckMeterNames() {
        List<String> meterNames = meterRegistry.getMeters().stream()
                .map(m -> m.getId().getName())
                .collect(Collectors.toList());

        Arrays.asList(
                "memory_used_heap",
                "cpu_available_processors",
                "classloader_loaded_classes_count",
                "cpu_system_load_average",
                "gc_time",
                "thread_count",
                "undertow.bytes.received"
        ).forEach(n -> Assert.assertTrue("Missing metric: " + n, meterNames.contains(n)));
    }
}
