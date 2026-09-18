/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.wildfly.extension.observability.shared.FilterModel;
import org.wildfly.extension.observability.shared.FilterModel.Condition;
import org.wildfly.extension.observability.shared.FilterModel.Field;
import org.wildfly.extension.observability.shared.FilterModel.Outcome;

/** Verifies Micrometer configuration snapshots caller-owned values correctly. */
public class WildFlyMicrometerConfigTest {

    /** Verifies immutable lists can configure subsystem exposure. */
    @Test
    public void acceptsImmutableSubsystemLists() {
        WildFlyMicrometerConfig config = new WildFlyMicrometerConfig.Builder()
                .exposedSubsystems(List.of("undertow")).build();

        assertTrue(config.getSubsystemFilter().test("undertow"));
        assertFalse(config.getSubsystemFilter().test("messaging-activemq"));
    }

    /** Verifies later caller mutations cannot change subsystem exposure. */
    @Test
    public void ignoresCallerListMutations() {
        List<String> exposedSubsystems = new ArrayList<>(List.of("undertow"));
        WildFlyMicrometerConfig config = new WildFlyMicrometerConfig.Builder()
                .exposedSubsystems(exposedSubsystems).build();

        exposedSubsystems.clear();
        exposedSubsystems.add("messaging-activemq");

        assertTrue(config.getSubsystemFilter().test("undertow"));
        assertFalse(config.getSubsystemFilter().test("messaging-activemq"));
    }

    /** Verifies wildcard exposure leaves the caller's list unchanged. */
    @Test
    public void preservesWildcardInCallerList() {
        List<String> exposedSubsystems = new ArrayList<>(List.of("*", "undertow"));
        WildFlyMicrometerConfig config = new WildFlyMicrometerConfig.Builder()
                .exposedSubsystems(exposedSubsystems).build();

        assertEquals(List.of("*", "undertow"), exposedSubsystems);
        assertTrue(config.getSubsystemFilter().test("undertow"));
        assertTrue(config.getSubsystemFilter().test("messaging-activemq"));
    }

    /** Verifies wildcard exposure works with an immutable input list. */
    @Test
    public void acceptsImmutableWildcardList() {
        WildFlyMicrometerConfig config = new WildFlyMicrometerConfig.Builder()
                .exposedSubsystems(List.of("*")).build();

        assertTrue(config.getSubsystemFilter().test("undertow"));
        assertTrue(config.getSubsystemFilter().test("messaging-activemq"));
    }

    /** Verifies an empty subsystem list exposes no subsystems. */
    @Test
    public void exposesNoSubsystemsForEmptyList() {
        WildFlyMicrometerConfig config = new WildFlyMicrometerConfig.Builder()
                .exposedSubsystems(List.of()).build();

        assertFalse(config.getSubsystemFilter().test("undertow"));
    }

    /** Verifies filter inputs are copied and the resulting list is immutable. */
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

    /** Creates the filter used by the snapshot test. */
    private static FilterModel filter() {
        return new FilterModel(Outcome.REJECT, Field.METER_NAME, Condition.EQUALS, false, "test");
    }
}
