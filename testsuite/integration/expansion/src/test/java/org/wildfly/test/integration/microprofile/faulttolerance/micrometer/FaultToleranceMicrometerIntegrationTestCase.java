/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.microprofile.faulttolerance.micrometer;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.setuptasks.MicrometerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.runner.RunWith;

/**
 * Test case to verify basic SmallRye Fault Tolerance integration with Micrometer. The test first invokes a REST
 * application which always times out, and Eclipse MP FT @Timeout kicks in with a fallback. Then we verify several of
 * the counters in the injected Micrometer's MeterRegistry.
 *
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
@ServerSetup(MicrometerSetupTask.class)
@TestcontainersRequired
public class FaultToleranceMicrometerIntegrationTestCase extends AbstractFaultToleranceMicrometerIntegrationTestCase {

    public FaultToleranceMicrometerIntegrationTestCase() {
        super(false);
    }

    @Deployment
    public static Archive<?> deploy() {
        return baseDeploy();
    }

}
