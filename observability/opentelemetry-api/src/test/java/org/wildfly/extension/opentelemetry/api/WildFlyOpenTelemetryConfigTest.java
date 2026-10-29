/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.opentelemetry.api;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.wildfly.extension.observability.shared.FilterModel;
import org.wildfly.extension.observability.shared.FilterModel.Condition;
import org.wildfly.extension.observability.shared.FilterModel.Field;
import org.wildfly.extension.observability.shared.FilterModel.Outcome;

public class WildFlyOpenTelemetryConfigTest {

    @Test
    public void testBuildSnapshotsMutableInputs() {
        Map<String, String> properties = new HashMap<>();
        properties.put("key", "before");
        List<FilterModel> filters = new ArrayList<>();
        filters.add(filter());

        WildFlyOpenTelemetryConfig config = new WildFlyOpenTelemetryConfig(properties, false, filters, true);

        properties.put("key", "after");
        filters.clear();

        org.junit.jupiter.api.Assertions.assertEquals("before", config.getProperties().get("key"));
        org.junit.jupiter.api.Assertions.assertEquals(1, config.getFilters().size());
        assertThrows(UnsupportedOperationException.class, () -> config.getProperties().put("other", "value"));
        assertThrows(UnsupportedOperationException.class, () -> config.getFilters().clear());
    }

    private static FilterModel filter() {
        return new FilterModel(Outcome.REJECT, Field.METER_NAME, Condition.EQUALS, false, "test");
    }
}
