/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.wildfly.extension.micrometer.MicrometerExtensionLogger.MICROMETER_LOGGER;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadDeadlockMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.wildfly.extension.micrometer.jmx.JmxMicrometerCollector;
import org.wildfly.extension.micrometer.metrics.MetricRegistration;
import org.wildfly.extension.micrometer.metrics.MicrometerCollector;
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;
import org.wildfly.extension.observability.shared.FilterModel;

public class MicrometerService {
    private final WildFlyMicrometerConfig micrometerConfig;
    private final LocalModelControllerClient modelControllerClient;
    private final ProcessStateNotifier processStateNotifier;
    private final WildFlyCompositeRegistry micrometerRegistry;

    private MicrometerCollector micrometerCollector;

    private MicrometerService(WildFlyMicrometerConfig micrometerConfig,
                              LocalModelControllerClient modelControllerClient,
                              ProcessStateNotifier processStateNotifier,
                              WildFlyCompositeRegistry micrometerRegistry) {
        this.micrometerConfig = micrometerConfig;
        this.modelControllerClient = modelControllerClient;
        this.processStateNotifier = processStateNotifier;
        this.micrometerRegistry = micrometerRegistry;
    }

    public void start() {
        registerMeterFilters();
        registerModelMetrics();

        if (micrometerConfig.exposeSystemMetrics()) {
            registerSystemMetrics();
            registerJmxMetrics();
        }
    }

    public WildFlyCompositeRegistry getMicrometerRegistry() {
        return micrometerRegistry;
    }

    public synchronized MetricRegistration collectResourceMetrics(final Resource resource,
                                                                  ImmutableManagementResourceRegistration mrr,
                                                                  Function<PathAddress, PathAddress> addressResolver) {
        return micrometerCollector.collectResourceMetrics(resource, mrr, addressResolver);
    }

    private void registerMeterFilters() {
        List<FilterModel> filters = micrometerConfig.getFilters();
        filters.stream().filter(filter -> filter.outcome() == FilterModel.Outcome.ACCEPT)
                .forEach(filter -> micrometerRegistry.config().meterFilter(toMeterFilter(filter)));
        filters.stream().filter(filter -> filter.outcome() == FilterModel.Outcome.REJECT)
                .forEach(filter -> micrometerRegistry.config().meterFilter(toMeterFilter(filter)));
    }

    private static MeterFilter toMeterFilter(FilterModel filter) {
        Predicate<Meter.Id> predicate = id ->
                filter.matches(id.getName(), id.getTags().stream()
                        .collect(Collectors.toMap(Tag::getKey, Tag::getValue)));
        return switch (filter.outcome()) {
            case ACCEPT -> MeterFilter.accept(predicate);
            case REJECT -> MeterFilter.deny(predicate);
        };
    }

    private void registerSystemMetrics() {
        new ClassLoaderMetrics().bindTo(micrometerRegistry);
        new JvmMemoryMetrics().bindTo(micrometerRegistry);
        new JvmGcMetrics().bindTo(micrometerRegistry);
        new ProcessorMetrics().bindTo(micrometerRegistry);
        new JvmThreadMetrics().bindTo(micrometerRegistry);
        new JvmThreadDeadlockMetrics().bindTo(micrometerRegistry);
    }

    private void registerModelMetrics() {
        micrometerCollector = new MicrometerCollector(modelControllerClient, processStateNotifier, micrometerRegistry,
                micrometerConfig.getSubsystemFilter());
    }

    private void registerJmxMetrics() {
        try {
            new JmxMicrometerCollector(micrometerRegistry).init();
        } catch (IOException e) {
            throw MICROMETER_LOGGER.failedInitializeJMXRegistrar(e);
        }
    }

    public static class Builder {
        private WildFlyMicrometerConfig micrometerConfig;
        private LocalModelControllerClient modelControllerClient;
        private ProcessStateNotifier processStateNotifier;
        private WildFlyCompositeRegistry micrometerRegistry;

        public Builder micrometerConfig(WildFlyMicrometerConfig micrometerConfig) {
            this.micrometerConfig = micrometerConfig;
            return this;
        }

        public Builder modelControllerClient(LocalModelControllerClient modelControllerClient) {
            this.modelControllerClient = modelControllerClient;
            return this;
        }

        public Builder processStateNotifier(ProcessStateNotifier processStateNotifier) {
            this.processStateNotifier = processStateNotifier;
            return this;
        }

        public Builder micrometerRegistry(WildFlyCompositeRegistry micrometerRegistry) {
            this.micrometerRegistry = micrometerRegistry;
            return this;
        }

        public MicrometerService build() {
            assert micrometerRegistry != null &&
                micrometerConfig != null &&
                modelControllerClient != null &&
                processStateNotifier != null;

            return new MicrometerService(micrometerConfig, modelControllerClient, processStateNotifier, micrometerRegistry);
        }
    }
}
