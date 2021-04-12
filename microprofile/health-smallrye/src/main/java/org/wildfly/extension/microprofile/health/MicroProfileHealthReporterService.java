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

import static org.wildfly.extension.microprofile.health.MicroProfileHealthSubsystemDefinition.HEALTH_SERVER_PROBE_CAPABILITY;
import static org.wildfly.extension.microprofile.health.MicroProfileHealthSubsystemDefinition.MICROPROFILE_HEALTH_REPORTER_CAPABILITY;

import java.util.function.Supplier;

import io.smallrye.health.ResponseProvider;
import io.smallrye.health.SmallRyeHealthReporter;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.dmr.Property;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.health.ServerProbe;
import org.wildfly.extension.health.ServerProbesService;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class MicroProfileHealthReporterService implements Service<MicroProfileHealthReporter> {

    private static MicroProfileHealthReporter healthReporter;
    private Supplier<ServerProbesService> serverProbesService;
    private String emptyLivenessChecksStatus;
    private String emptyReadinessChecksStatus;

    static void install(OperationContext context, String emptyLivenessChecksStatus, String emptyReadinessChecksStatus) {

        CapabilityServiceBuilder<?> serviceBuilder = context.getCapabilityServiceTarget()
                .addCapability(RuntimeCapability.Builder.of(MICROPROFILE_HEALTH_REPORTER_CAPABILITY, SmallRyeHealthReporter.class).build());

        Supplier<ServerProbesService> serverProbesService = serviceBuilder.requires(ServiceName.parse(HEALTH_SERVER_PROBE_CAPABILITY));

        serviceBuilder.setInstance(new MicroProfileHealthReporterService(serverProbesService, emptyLivenessChecksStatus, emptyReadinessChecksStatus))
                .install();
    }

    private MicroProfileHealthReporterService(Supplier<ServerProbesService> serverProbesService, String emptyLivenessChecksStatus, String emptyReadinessChecksStatus) {
        this.serverProbesService = serverProbesService;
        this.emptyLivenessChecksStatus = emptyLivenessChecksStatus;
        this.emptyReadinessChecksStatus = emptyReadinessChecksStatus;
    }

    @Override
    public void start(StartContext context) {
        // MicroProfile Health supports the mp.health.disable-default-procedures to let users disable any vendor procedures
        final boolean defaultServerProceduresDisabled = ConfigProvider.getConfig().getOptionalValue("mp.health.disable-default-procedures", Boolean.class).orElse(false);
        healthReporter = new MicroProfileHealthReporter(emptyLivenessChecksStatus, emptyReadinessChecksStatus, defaultServerProceduresDisabled);

        if (!defaultServerProceduresDisabled) {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            for (ServerProbe serverProbe : serverProbesService.get().getServerProbes()) {
                healthReporter.addServerReadinessCheck(wrap(serverProbe), tccl);
            }
        }

        HealthCheckResponse.setResponseProvider(new ResponseProvider());
    }

    @Override
    public void stop(StopContext context) {
        healthReporter = null;
        HealthCheckResponse.setResponseProvider(null);
    }

    @Override
    public MicroProfileHealthReporter getValue() {
        return healthReporter;
    }

    static HealthCheck wrap(ServerProbe delegate) {
        return new HealthCheck() {
            @Override
            public HealthCheckResponse call() {
                ServerProbe.Outcome outcome = delegate.getOutcome();

                HealthCheckResponseBuilder check = HealthCheckResponse.named(delegate.getName())
                        .status(outcome.isSuccess());
                if (outcome.getData().isDefined()) {
                    for (Property property : outcome.getData().asPropertyList()) {
                        check.withData(property.getName(), property.getValue().asString());
                    }
                }
                return check.build();
            }
        };
    }
}
