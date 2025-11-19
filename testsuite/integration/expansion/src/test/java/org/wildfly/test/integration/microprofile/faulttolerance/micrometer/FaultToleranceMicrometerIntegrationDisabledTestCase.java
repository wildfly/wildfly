/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.microprofile.faulttolerance.micrometer;

import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.setuptasks.MicrometerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.runner.RunWith;

/**
 * Variant of the {@link FaultToleranceMicrometerIntegrationTestCase} test which disabled Micrometer metrics using an MP Config property.
 *
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
@TestcontainersRequired
@ServerSetup({MicrometerSetupTask.class})
public class FaultToleranceMicrometerIntegrationDisabledTestCase extends AbstractFaultToleranceMicrometerIntegrationTestCase {

    public FaultToleranceMicrometerIntegrationDisabledTestCase() {
        super(true);
    }

    private static final String MP_CONFIG = "smallrye.faulttolerance.micrometer.disabled=true\n" +
            "smallrye.faulttolerance.opentelemetry.disabled=true\n";

    @Deployment
    public static Archive<?> deploy() {
        return baseDeploy().addAsManifestResource(new StringAsset(MP_CONFIG), "microprofile-config.properties");
    }

}
