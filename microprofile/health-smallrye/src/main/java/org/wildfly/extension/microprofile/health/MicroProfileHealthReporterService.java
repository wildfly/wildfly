/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.health;

import java.util.function.Consumer;
import java.util.function.Supplier;

import io.smallrye.health.ResponseProvider;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.jboss.dmr.Property;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.health.ServerProbe;
import org.wildfly.extension.health.ServerProbesService;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class MicroProfileHealthReporterService implements Service {

    private final Consumer<MicroProfileHealthReporter> reporter;
    private final Supplier<ServerProbesService> serverProbesService;
    private final String emptyLivenessChecksStatus;
    private final String emptyReadinessChecksStatus;
    private final String emptyStartupChecksStatus;

    MicroProfileHealthReporterService(Consumer<MicroProfileHealthReporter> reporter, Supplier<ServerProbesService> serverProbesService, String emptyLivenessChecksStatus,
            String emptyReadinessChecksStatus, String emptyStartupChecksStatus) {
        this.reporter = reporter;
        this.serverProbesService = serverProbesService;
        this.emptyLivenessChecksStatus = emptyLivenessChecksStatus;
        this.emptyReadinessChecksStatus = emptyReadinessChecksStatus;
        this.emptyStartupChecksStatus = emptyStartupChecksStatus;
    }

    @Override
    public void start(StartContext context) {
        // MicroProfile Health supports the mp.health.disable-default-procedures to let users disable any vendor procedures,
        // here the property value is read and stored when the runtime is starting
        final boolean defaultServerProceduresDisabled = ConfigProvider.getConfig().getOptionalValue("mp.health.disable-default-procedures", Boolean.class).orElse(false);
        // MicroProfile Health supports the mp.health.default.readiness.empty.response to let users specify default empty readiness responses
        final String defaultReadinessEmptyResponse = ConfigProvider.getConfig().getOptionalValue("mp.health.default.readiness.empty.response", String.class).orElse("DOWN");
        // MicroProfile Health supports the mp.health.default.startup.empty.response to let users specify default empty startup responses
        final String defaultStartupEmptyResponse = ConfigProvider.getConfig().getOptionalValue("mp.health.default.startup.empty.response", String.class).orElse("DOWN");
        MicroProfileHealthReporter healthReporter = new MicroProfileHealthReporter(emptyLivenessChecksStatus, emptyReadinessChecksStatus,
            emptyStartupChecksStatus, defaultServerProceduresDisabled,
            defaultReadinessEmptyResponse, defaultStartupEmptyResponse);

        if (!defaultServerProceduresDisabled) {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            for (ServerProbe serverProbe : serverProbesService.get().getServerProbes()) {
                healthReporter.addServerReadinessCheck(wrap(serverProbe), tccl);
            }
        }

        HealthCheckResponse.setResponseProvider(new ResponseProvider());
        this.reporter.accept(healthReporter);
    }

    @Override
    public void stop(StopContext context) {
        HealthCheckResponse.setResponseProvider(null);
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
