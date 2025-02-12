/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.microprofile.faulttolerance.micrometer.deployment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Timeout;

/**
 * A service that always times out!
 *
 * @author Radoslav Husar
 */
@Path("/timeout")
@ApplicationScoped
public class TimeoutService {

    @Fallback(fallbackMethod = "fallback")
    @Timeout(100)
    @GET
    public String alwaysTimeout() {
        try {
            // Note that sleep duration is longer than the timeout above.
            // Also note that this needs to be more than slightly higher than the timeout on slow/overloaded systems.
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            // Ignore.
        }
        return "standard method processing";
    }

    public String fallback() {
        return "fallback method called";
    }
}
