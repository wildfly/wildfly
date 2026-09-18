/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.wildfly.extension.observability.shared.FilterModel;
import org.wildfly.extension.observability.shared.FilterModel.Condition;
import org.wildfly.extension.observability.shared.FilterModel.Field;
import org.wildfly.extension.observability.shared.FilterModel.Outcome;

public class WildFlyMicrometerConfigTest {

    @Test
    public void testBuilderSnapshotsMutableInputs() {
        List<FilterModel> filters = new ArrayList<>();
        filters.add(filter());

        WildFlyMicrometerConfig config = new WildFlyMicrometerConfig.Builder()
                .exposedSubsystems(new ArrayList<>(List.of("*")))
                .addFilters(filters)
                .build();

        filters.clear();

        assertEquals(1, config.getFilters().size());
        assertThrows(UnsupportedOperationException.class, () -> config.getFilters().clear());
    }

    private static FilterModel filter() {
        return new FilterModel(Outcome.REJECT, Field.METER_NAME, Condition.EQUALS, false, "test");
    }
}
