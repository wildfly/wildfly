/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.faulttolerance.tck;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.Extension;

import io.opentelemetry.api.OpenTelemetry;

/**
 * Adapted from SmallRye Fault Tolerance project.
 *
 * @author Radoslav Husar
 */
public class EagerOpenTelemetryBean implements Extension {

    public void afterDeploymentValidation(@Observes AfterDeploymentValidation ignore) {
        // Required to make sure the OpenTelemetry bean is instantiated eagerly,
        // which in turn triggers initialization of the MP FT TCK supporting infrastructure
        // which is required in the tests.
        // Otherwise, all telemetry tests fail with:
        // java.lang.IllegalStateException: InMemoryMetricReader has not been registered
        CDI.current().select(OpenTelemetry.class).get();
    }

}
