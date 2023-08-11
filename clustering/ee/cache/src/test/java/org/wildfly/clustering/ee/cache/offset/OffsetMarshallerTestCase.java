/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.offset;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.junit.Test;
import org.wildfly.clustering.marshalling.MarshallingTester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for {@link Offset} marshalling.
 * @author Paul Ferraro
 */
public class OffsetMarshallerTestCase {

    @Test
    public void duration() throws IOException {
        MarshallingTester<Offset<Duration>> tester = ProtoStreamTesterFactory.INSTANCE.createTester();

        tester.test(Offset.forDuration(Duration.ofSeconds(1)));
    }

    @Test
    public void instant() throws IOException {
        MarshallingTester<Offset<Instant>> tester = ProtoStreamTesterFactory.INSTANCE.createTester();

        tester.test(Offset.forInstant(Duration.ofSeconds(1)));
    }
}
