/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import org.junit.Test;
import org.wildfly.extension.observability.shared.FilterModel;
import org.wildfly.extension.observability.shared.FilterModel.Condition;
import org.wildfly.extension.observability.shared.FilterModel.Field;
import org.wildfly.extension.observability.shared.FilterModel.Outcome;

public class FilterTest {

    private static final Meter.Id JVM_MEMORY = new Meter.Id("jvm.memory.used",
            Tags.of("env", "prod"), null, null, Meter.Type.GAUGE);
    private static final Meter.Id HTTP_SERVER = new Meter.Id("http.server.requests",
            Tags.of("method", "GET", "status", "200"), null, null, Meter.Type.COUNTER);

    static MeterFilter toMeterFilter(FilterModel filter) {
        Predicate<Meter.Id> predicate = id ->
                filter.matches(id.getName(), id.getTags().stream()
                        .collect(Collectors.toMap(Tag::getKey, Tag::getValue)));
        return switch (filter.outcome()) {
            case ACCEPT -> MeterFilter.accept(predicate);
            case REJECT -> MeterFilter.deny(predicate);
        };
    }

    @Test
    public void testToMeterFilterReturnsNonNull() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.METER_NAME, Condition.EQUALS, false, "test");
        assertNotNull(toMeterFilter(filter));
    }

    @Test
    public void testRejectMatchProducesDeny() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.METER_NAME, Condition.STARTS_WITH, false, "jvm.");
        MeterFilter mf = toMeterFilter(filter);
        assertEquals(MeterFilterReply.DENY, mf.accept(JVM_MEMORY));
    }

    @Test
    public void testRejectNoMatchProducesNeutral() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.METER_NAME, Condition.STARTS_WITH, false, "jvm.");
        MeterFilter mf = toMeterFilter(filter);
        assertEquals(MeterFilterReply.NEUTRAL, mf.accept(HTTP_SERVER));
    }

    @Test
    public void testAcceptMatchProducesAccept() {
        FilterModel filter = new FilterModel(Outcome.ACCEPT, Field.METER_NAME, Condition.STARTS_WITH, false, "http.");
        MeterFilter mf = toMeterFilter(filter);
        assertEquals(MeterFilterReply.ACCEPT, mf.accept(HTTP_SERVER));
    }

    @Test
    public void testAcceptNoMatchProducesNeutral() {
        FilterModel filter = new FilterModel(Outcome.ACCEPT, Field.METER_NAME, Condition.STARTS_WITH, false, "http.");
        MeterFilter mf = toMeterFilter(filter);
        assertEquals(MeterFilterReply.NEUTRAL, mf.accept(JVM_MEMORY));
    }

    @Test
    public void testNegateInvertsBeforeOutcome() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.METER_NAME, Condition.STARTS_WITH, true, "jvm.");
        MeterFilter mf = toMeterFilter(filter);
        assertEquals(MeterFilterReply.NEUTRAL, mf.accept(JVM_MEMORY));
        assertEquals(MeterFilterReply.DENY, mf.accept(HTTP_SERVER));
    }
}
