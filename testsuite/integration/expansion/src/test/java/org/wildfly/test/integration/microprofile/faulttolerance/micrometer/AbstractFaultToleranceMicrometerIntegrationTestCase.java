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

import jakarta.inject.Inject;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.search.Search;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.test.integration.microprofile.faulttolerance.micrometer.deployment.FaultTolerantApplication;
import org.wildfly.test.integration.microprofile.faulttolerance.micrometer.deployment.TimeoutService;

/**
 * Test case base to verify basic SmallRye Fault Tolerance integration with Micrometer.
 *
 * @author Radoslav Husar
 */
public abstract class AbstractFaultToleranceMicrometerIntegrationTestCase {

    private final boolean disabled;

    public AbstractFaultToleranceMicrometerIntegrationTestCase(boolean disabled) {
        this.disabled = disabled;
    }

    public static WebArchive baseDeploy() {
        return ShrinkWrap.create(WebArchive.class, FaultToleranceMicrometerIntegrationDisabledTestCase.class.getSimpleName() + ".war")
                .addClasses(ServerSetupTask.class, AbstractFaultToleranceMicrometerIntegrationTestCase.class)
                .addPackage(FaultTolerantApplication.class.getPackage())
                .addAsWebInfResource(FaultToleranceMicrometerIntegrationDisabledTestCase.class.getPackage(), "web.xml", "web.xml")
                .addAsWebInfResource(FaultToleranceMicrometerIntegrationDisabledTestCase.class.getPackage(), "beans.xml", "beans.xml");
    }

    // Let's use a slightly higher number of invocations, so we can at times differentiate between stale read and other problems
    private static final int INVOCATION_COUNT = 10;

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
        // Use specific tags to lookup proper counters
        // For reference, review definitions in class io.smallrye.faulttolerance.core.metrics.MetricsConstants
        Iterable<Tag> timeoutServiceMethodTag = Collections.singleton(Tag.of("method", TimeoutService.class.getName().concat(".alwaysTimeout")));

        // First verify total invocation count for the method + value returned + fallback applied
        Collection<Counter> counters = Search.in(meterRegistry).name("ft.invocations.total").tags(timeoutServiceMethodTag).tags("result", "valueReturned", "fallback", "applied").counters();
        Assert.assertEquals(disabled ? 0 : 1, counters.size());
        if (!disabled) {
            Assert.assertEquals(INVOCATION_COUNT, counters.iterator().next().count(), 0);
        }

        // Verify the number of timeouts being equal to number of invocations
        counters = Search.in(meterRegistry).name("ft.timeout.calls.total").tags(timeoutServiceMethodTag).tags("timedOut", "true").counters();
        Assert.assertEquals(disabled ? 0 : 1, counters.size());
        if (!disabled) {
            Assert.assertEquals(INVOCATION_COUNT, counters.iterator().next().count(), 0);
        }

        // Verify the number of successful invocations to be none, since it always fails
        counters = Search.in(meterRegistry).name("ft.timeout.calls.total").tags(timeoutServiceMethodTag).tags("timedOut", "false").counters();
        Assert.assertEquals(disabled ? 0 : 1, counters.size());

        if (!disabled) {
            Assert.assertEquals(0, counters.iterator().next().count(), 0);
        }
    }

}
