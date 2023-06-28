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
package org.wildfly.test.integration.microprofile.faulttolerance.micrometer;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.faulttolerance.micrometer.deployment.FaultTolerantApplication;
import org.wildfly.test.integration.observability.micrometer.MicrometerSetupTask;

/**
 * Test case to verify SmallRye Fault Tolerance integration with Micrometer. This invokes a REST application which always
 * times out and Eclipse MP FT @Timeout kicks in, then we verify the counter in the injected Micrometer's MeterRegistry.
 *
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
@ServerSetup(MicrometerSetupTask.class)
public class FaultToleranceMicrometerIntegrationTestCase {

    // Let's use a slightly higher number of invocations, so we can at times differentiate between stale read and or other problems
    private static final int INVOCATION_COUNT = 10;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, FaultToleranceMicrometerIntegrationTestCase.class.getSimpleName() + ".war")
                .addClasses(ServerSetupTask.class)
                .addPackage(FaultTolerantApplication.class.getPackage())
                .addAsWebInfResource(FaultToleranceMicrometerIntegrationTestCase.class.getPackage(), "web.xml", "web.xml")
                .addAsWebInfResource(FaultToleranceMicrometerIntegrationTestCase.class.getPackage(), "beans.xml", "beans.xml");
    }

    @ArquillianResource
    private URL url;

    @Inject
    private MeterRegistry meterRegistry;

    @Test
    @InSequence(1)
    public void testClearInjectedRegistry() {
        Assert.assertNotNull(meterRegistry);

        meterRegistry.clear();
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void makeRequests() throws IOException, ExecutionException, TimeoutException {
        for (int i = 0; i < INVOCATION_COUNT; i++) {
            HttpRequest.get(url.toString() + "app/timeout", 10, TimeUnit.SECONDS);
        }
    }

    @Test
    @InSequence(3)
    public void checkCounter() {
        Counter counterInvocations = meterRegistry.get("ft.invocations.total").counter();
        Assert.assertEquals(0, counterInvocations.count(), 0);

        Counter counterTimeouts = meterRegistry.get("ft.timeout.calls.total").counter();
        Assert.assertEquals(INVOCATION_COUNT, counterTimeouts.count(), 0);
    }

}
