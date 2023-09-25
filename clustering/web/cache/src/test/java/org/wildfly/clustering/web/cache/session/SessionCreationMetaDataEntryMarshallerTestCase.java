/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.MarshallingTester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for {@link SessionCreationMetaDataEntryExternalizer}.
 * @author Paul Ferraro
 */
public class SessionCreationMetaDataEntryMarshallerTestCase {

    @Test
    public void test() throws IOException {
        MarshallingTester<SessionCreationMetaDataEntry<Object>> tester = ProtoStreamTesterFactory.INSTANCE.createTester();

        SessionCreationMetaData metaData = new SimpleSessionCreationMetaData(Instant.now());
        SessionCreationMetaDataEntry<Object> entry = new SessionCreationMetaDataEntry<>(metaData);

        // Default max-inactive-interval
        metaData.setTimeout(Duration.ofMinutes(30));
        tester.test(entry, SessionCreationMetaDataEntryMarshallerTestCase::assertEquals);

        // Custom max-inactive-interval
        metaData.setTimeout(Duration.ofMinutes(10));
        tester.test(entry, SessionCreationMetaDataEntryMarshallerTestCase::assertEquals);
    }

    static void assertEquals(SessionCreationMetaDataEntry<Object> entry1, SessionCreationMetaDataEntry<Object> entry2) {
        // Compare only to millisecond precision
        Assert.assertEquals(entry1.getMetaData().getCreationTime().toEpochMilli(), entry2.getMetaData().getCreationTime().toEpochMilli());
        Assert.assertEquals(entry1.getMetaData().getTimeout(), entry2.getMetaData().getTimeout());
    }
}
