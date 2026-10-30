/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.observability.setuptasks.MicrometerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.test.integration.observability.JaxRsActivator;


@ExtendWith(ArquillianExtension.class)
@ServerSetup(MicrometerSetupTask.class)
@TestcontainersRequired
public class BasicMicrometerTestCase {
    @Inject
    private MeterRegistry meterRegistry;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "micrometer-test.war")
                .addClasses(
                        JaxRsActivator.class,
                        MicrometerResource.class)
                .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml");
    }

    @Test
    public void testInjection() {
        Assertions.assertNotNull(meterRegistry);
    }
}
