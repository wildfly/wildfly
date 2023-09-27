/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.faces.view;

import java.io.IOException;

import jakarta.faces.view.Location;

import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

import org.junit.Assert;

/**
 * Validates marshalling of a {@link Location}.
 * @author Paul Ferraro
 */
public class LocationMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<Location> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new Location("/foo", -1, -1), LocationMarshallerTestCase::assertEquals);
        tester.test(new Location("/var", 11, 12), LocationMarshallerTestCase::assertEquals);
    }

    static void assertEquals(Location location1, Location location2) {
        Assert.assertEquals(location1.getPath(), location2.getPath());
        Assert.assertEquals(location1.getLine(), location2.getLine());
        Assert.assertEquals(location1.getColumn(), location2.getColumn());
    }
}
