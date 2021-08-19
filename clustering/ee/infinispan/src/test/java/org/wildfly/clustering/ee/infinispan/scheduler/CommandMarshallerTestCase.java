/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
