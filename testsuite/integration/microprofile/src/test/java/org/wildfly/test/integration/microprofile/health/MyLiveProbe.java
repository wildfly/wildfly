/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.microprofile.health;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import javax.inject.Inject;
import javax.inject.Provider;

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

