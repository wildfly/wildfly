/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.collector;

import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.resource.v1.Resource;
import org.jboss.as.test.shared.observability.signals.SimpleMetric;

public final class CollectorUtil {
    private static final Boolean loggingEnabled; // Default: null/false

    static {
        loggingEnabled =
                Boolean.parseBoolean(System.getenv().get("TC_LOGGING")) ||
                        Boolean.parseBoolean(System.getProperty("testsuite.integration.container.logging")) ||
                        Boolean.parseBoolean(System.getProperty("testsuite.integration.container.InMemoryCollector.logging"));
    }

    private CollectorUtil() {
    }

    public static void debugLog(String message) {
        if (loggingEnabled) {
            System.err.println("[InMemoryCollector] " + message);
        }
    }

    public static String fromByteString(ByteString bs) {
        return HexFormat.of().formatHex(bs.toByteArray());
    }

    public static Map<String, String> fromKeyValueList(List<KeyValue> kvList) {
        return kvList.stream().collect(Collectors.toMap(KeyValue::getKey, kv -> kv.getValue().getStringValue(),
                (a, b) -> b, HashMap::new));
    }

    public static void fromMetric(Consumer<SimpleMetric> consumer, Resource rm, InstrumentationScope sm, Metric m) {
        if (m.hasSum()) {
            m.getSum().getDataPointsList().forEach(dp -> {
                consumer.accept(createMeter(rm, sm, m,
                        dp.hasAsDouble() ? Double.toString(dp.getAsDouble()) : Long.toString(dp.getAsInt()),
                        "sum",
                        fromKeyValueList(dp.getAttributesList())));
            });
        } else if (m.hasGauge()) {
            m.getGauge().getDataPointsList().forEach(dp -> {
                consumer.accept(createMeter(rm, sm, m, dp.hasAsDouble() ? Double.toString(dp.getAsDouble()) : Long.toString(dp.getAsInt()),
                        "summary",
                        fromKeyValueList(dp.getAttributesList())));
            });
        }  else if (m.hasSummary()) {
            m.getSummary().getDataPointsList().forEach(dp -> {
                consumer.accept(createMeter(rm, sm, m, Double.toString(dp.getSum()), "summary",
                        fromKeyValueList(dp.getAttributesList())));
            });
        } else if (m.hasHistogram() || m.hasExponentialHistogram()) {
            // TODO: I'm not sure what this looks like, and we don't use them in WF so we can probably safely ignore
        }
    }

    private static SimpleMetric createMeter(Resource resource,
                                     InstrumentationScope scope,
                                     Metric m,
                                     String value,
                                     String type,
                                     Map<String, String> tags) {
        return new SimpleMetric(
                m.getName(),
                m.getDescription(),
                value,
                type,
                m.getUnit(),
                fromKeyValueList(resource.getAttributesList()),
                scope.getName(),
                tags);
    }
}
