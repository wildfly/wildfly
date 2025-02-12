/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Startup;

/**
 * @author <a href="http://xstefank.io/">Martin Stefanko</a> (c) 2019 Red Hat inc.
 */
@Startup
public class SuccessfulStartupProbe implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("successfulStartupProbe")
                .up()
                .build();
    }
}
