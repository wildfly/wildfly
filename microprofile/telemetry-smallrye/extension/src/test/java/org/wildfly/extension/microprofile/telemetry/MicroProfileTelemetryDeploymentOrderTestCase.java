/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.telemetry;

import static org.junit.Assert.assertTrue;

import org.jboss.as.server.deployment.Phase;
import org.junit.Test;

/** Verifies MicroProfile Telemetry runs after the OpenTelemetry deployment processor. */
public class MicroProfileTelemetryDeploymentOrderTestCase {

    /** Verifies configuration production occurs after OpenTelemetry deployment setup. */
    @Test
    public void testDeploymentProcessorRunsAfterOpenTelemetry() {
        assertTrue("MicroProfile Telemetry must run after OpenTelemetry",
                Phase.POST_MODULE_MICROPROFILE_TELEMETRY > Phase.POST_MODULE_OPENTELEMETRY);
    }
}
