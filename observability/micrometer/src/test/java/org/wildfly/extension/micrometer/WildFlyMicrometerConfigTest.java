/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/** Verifies subsystem exposure is stable and does not modify caller-owned configuration. */
public class WildFlyMicrometerConfigTest {

    /** Verifies immutable lists can configure exposure without requiring a mutable input. */
    @Test
    public void acceptsImmutableSubsystemLists() {
        WildFlyMicrometerConfig config = new WildFlyMicrometerConfig.Builder()
                .exposedSubsystems(List.of("undertow")).build();

        assertTrue(config.getSubsystemFilter().test("undertow"));
        assertFalse(config.getSubsystemFilter().test("messaging-activemq"));
    }

    /** Verifies later caller mutations cannot change the configured subsystem exposure. */
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

    /** Verifies wildcard exposure leaves the caller's configuration available for reuse. */
    @Test
    public void preservesWildcardInCallerList() {
        List<String> exposedSubsystems = new ArrayList<>(List.of("*", "undertow"));
        WildFlyMicrometerConfig config = new WildFlyMicrometerConfig.Builder()
                .exposedSubsystems(exposedSubsystems).build();

        assertEquals(List.of("*", "undertow"), exposedSubsystems);
        assertTrue(config.getSubsystemFilter().test("undertow"));
        assertTrue(config.getSubsystemFilter().test("messaging-activemq"));
    }

    /** Verifies immutable wildcard configuration exposes every subsystem. */
    @Test
    public void acceptsImmutableWildcardList() {
        WildFlyMicrometerConfig config = new WildFlyMicrometerConfig.Builder()
                .exposedSubsystems(List.of("*")).build();

        assertTrue(config.getSubsystemFilter().test("undertow"));
        assertTrue(config.getSubsystemFilter().test("messaging-activemq"));
    }

    /** Verifies an empty configuration exposes no subsystems. */
    @Test
    public void exposesNoSubsystemsForEmptyList() {
        WildFlyMicrometerConfig config = new WildFlyMicrometerConfig.Builder()
                .exposedSubsystems(List.of()).build();

        assertFalse(config.getSubsystemFilter().test("undertow"));
    }
}
