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

package org.wildfly.extension.microprofile.health;

import static org.wildfly.extension.microprofile.health.MicroProfileHealthSubsystemDefinition.HEALTH_REPORTER_CAPABILITY;

import io.smallrye.health.ResponseProvider;
import io.smallrye.health.SmallRyeHealthReporter;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class HealthReporterService implements Service<SmallRyeHealthReporter> {

    private SmallRyeHealthReporter healthReporter;

    static void install(OperationContext context) {
        context.getCapabilityServiceTarget()
                .addCapability(RuntimeCapability.Builder.of(HEALTH_REPORTER_CAPABILITY, SmallRyeHealthReporter.class).build(),
                        new HealthReporterService())
                .install();
    }

    private HealthReporterService() {
    }

    @Override
    public void start(StartContext context) {
        HealthCheckResponse.setResponseProvider(new ResponseProvider());
        this.healthReporter = new SmallRyeHealthReporter();
    }

    @Override
    public void stop(StopContext context) {
        this.healthReporter = null;
        HealthCheckResponse.setResponseProvider(null);
    }

    @Override
    public SmallRyeHealthReporter getValue() {
        return healthReporter;
    }
}
