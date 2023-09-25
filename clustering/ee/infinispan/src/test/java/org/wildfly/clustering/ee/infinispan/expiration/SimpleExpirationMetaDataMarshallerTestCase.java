/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan.expiration;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.ee.expiration.ExpirationMetaData;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Marshaller test for {@link SimpleExpirationMetaData}.
 * @author Paul Ferraro
 */
public class SimpleExpirationMetaDataMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<ExpirationMetaData> tester = ProtoStreamTesterFactory.INSTANCE.createTester();

        tester.test(new SimpleExpirationMetaData(Duration.ofMinutes(30), Instant.EPOCH), this::assertEquals);
        tester.test(new SimpleExpirationMetaData(Duration.ofSeconds(600), Instant.now()), this::assertEquals);
    }

    private void assertEquals(ExpirationMetaData expected, ExpirationMetaData actual) {
        Assert.assertEquals(expected.getTimeout(), actual.getTimeout());
        Assert.assertEquals(expected.getLastAccessTime(), actual.getLastAccessTime());
    }
}
