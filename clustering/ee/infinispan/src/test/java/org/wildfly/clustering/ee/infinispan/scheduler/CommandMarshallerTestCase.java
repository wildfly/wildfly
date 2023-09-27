/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan.scheduler;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for marshalling scheduler commands.
 * @author Paul Ferraro
 */
public class CommandMarshallerTestCase {

    private final MarshallingTesterFactory factory = ProtoStreamTesterFactory.INSTANCE;

    @Test
    public void testScheduleWithLocalMetaDataCommand() throws IOException {
        Tester<ScheduleWithTransientMetaDataCommand<String, String>> tester = this.factory.createTester();

        tester.test(new ScheduleWithTransientMetaDataCommand<>("foo"), this::assertEquals);
        tester.test(new ScheduleWithTransientMetaDataCommand<>("foo", "bar"), this::assertEquals);
    }

    <I, M> void assertEquals(ScheduleWithTransientMetaDataCommand<I, M> expected, ScheduleWithTransientMetaDataCommand<I, M> actual) {
        Assert.assertEquals(expected.getId(), actual.getId());
        Assert.assertNull(actual.getMetaData());
    }

    @Test
    public void testCancelCommand() throws IOException {
        Tester<CancelCommand<String, Object>> tester = this.factory.createTester();

        tester.test(new CancelCommand<>("foo"), this::assertEquals);
    }

    <I, M> void assertEquals(CancelCommand<I, M> expected, CancelCommand<I, M> actual) {
        Assert.assertEquals(expected.getId(), actual.getId());
    }

    @Test
    public void testScheduleWithMetaDataCommand() throws IOException {
        Tester<ScheduleWithMetaDataCommand<String, String>> tester = this.factory.createTester();

        tester.test(new ScheduleWithMetaDataCommand<>("foo", "bar"), this::assertEquals);
    }

    <I, M> void assertEquals(ScheduleWithMetaDataCommand<I, M> expected, ScheduleWithMetaDataCommand<I, M> actual) {
        Assert.assertEquals(expected.getId(), actual.getId());
        Assert.assertEquals(expected.getMetaData(), actual.getMetaData());
    }
}
