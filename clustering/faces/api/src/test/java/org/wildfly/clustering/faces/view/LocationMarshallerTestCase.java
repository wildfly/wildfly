/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.faces.view;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

import jakarta.faces.view.Location;

/**
 * Validates marshalling of a {@link Location}.
 * @author Paul Ferraro
 */
public class LocationMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<Location> tester = factory.createTester(LocationMarshallerTestCase::assertEquals);
        tester.accept(new Location("/foo", -1, -1));
        tester.accept(new Location("/var", 11, 12));
    }

    static void assertEquals(Location location1, Location location2) {
        Assertions.assertEquals(location1.getPath(), location2.getPath());
        Assertions.assertEquals(location1.getLine(), location2.getLine());
        Assertions.assertEquals(location1.getColumn(), location2.getColumn());
    }
}
