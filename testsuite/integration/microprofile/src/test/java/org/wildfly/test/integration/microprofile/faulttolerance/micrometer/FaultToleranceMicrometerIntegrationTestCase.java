/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.microprofile.faulttolerance.micrometer;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.search.Search;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testcontainers.api.DockerRequired;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.AssumptionViolatedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.faulttolerance.micrometer.deployment.FaultTolerantApplication;
import org.wildfly.test.integration.microprofile.faulttolerance.micrometer.deployment.TimeoutService;
import org.wildfly.test.integration.observability.setuptask.MicrometerSetupTask;

/**
 * Test case to verify basic SmallRye Fault Tolerance integration with Micrometer. The test first invokes a REST
 * application which always times out, and Eclipse MP FT @Timeout kicks in with a fallback. Then we verify several of
 * the counters in the injected Micrometer's MeterRegistry.
 *
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
@ServerSetup(MicrometerSetupTask.class)
@DockerRequired(AssumptionViolatedException.class)
public class FaultToleranceMicrometerIntegrationTestCase {

    // Let's use a slightly higher number of invocations, so we can at times differentiate between stale read and other problems
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
    public void clearInjectedRegistry() {
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
    public void checkCounters() {
        // Use specific tags to lookup proper counters - review definitions in io.smallrye.faulttolerance.metrics.MicrometerProvider for reference
        Iterable<Tag> timeoutServiceMethodTag = Collections.singleton(Tag.of("method", TimeoutService.class.getName().concat(".alwaysTimeout")));

        // First verify total invocation count for the method + value returned + fallback applied
        Collection<Counter> counters = Search.in(meterRegistry).name("ft.invocations.total").tags(timeoutServiceMethodTag).tags("result", "valueReturned", "fallback", "applied").counters();
        Assert.assertEquals(1, counters.size());
        Assert.assertEquals(INVOCATION_COUNT, counters.iterator().next().count(), 0);

        // Verify number of timeouts being equal to number of invocations
        counters = Search.in(meterRegistry).name("ft.timeout.calls.total").tags(timeoutServiceMethodTag).tags("timedOut", "true").counters();
        Assert.assertEquals(1, counters.size());
        Assert.assertEquals(INVOCATION_COUNT, counters.iterator().next().count(), 0);

        // Verify number of successful invocations to be none, since it always fails
        counters = Search.in(meterRegistry).name("ft.timeout.calls.total").tags(timeoutServiceMethodTag).tags("timedOut", "false").counters();
        Assert.assertEquals(1, counters.size());
        Assert.assertEquals(0, counters.iterator().next().count(), 0);
    }

}
