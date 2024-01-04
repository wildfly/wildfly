/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
@Liveness
public class MyLiveProbe implements HealthCheck {

    // Inject a property whose value is configured in a ConfigSource
    // inside the deployment to check that MP Config properly injects properties
    // when HealthChecks are called in WildFly management endpoints.
    @Inject
    @ConfigProperty
    Provider<Boolean> propertyConfiguredByTheDeployment;

    static boolean up = true;

    @Override
    public HealthCheckResponse call() {

        if (!propertyConfiguredByTheDeployment.get()) {
            return HealthCheckResponse.named("myLiveProbe")
                    .down()
                    .build();
        }

        return HealthCheckResponse.named("myLiveProbe")
                .status(up)
                .build();
    }
}

