/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.MarshallingTester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for {@link SessionAccessMetaDataExternalizer}.
 * @author Paul Ferraro
 */
public class DefaultSessionAccessMetaDataEntryMarshallerTestCase {

    @Test
    public void test() throws IOException {
        MarshallingTester<DefaultSessionAccessMetaDataEntry> tester = ProtoStreamTesterFactory.createTester(List.of(new FineSessionMetaDataSerializationContextInitializer()));

        DefaultSessionAccessMetaDataEntry metaData = new DefaultSessionAccessMetaDataEntry();

        // New session
        metaData.setLastAccessDuration(Duration.ZERO, Duration.ofNanos(100_000_000));
        tester.test(metaData, DefaultSessionAccessMetaDataEntryMarshallerTestCase::assertEquals);

        // Existing session, sub-second response time
        metaData.setLastAccessDuration(Duration.ofSeconds(60 * 5), Duration.ofNanos(100_000_000));
        tester.test(metaData, DefaultSessionAccessMetaDataEntryMarshallerTestCase::assertEquals);

        // Existing session, +1 second response time
        metaData.setLastAccessDuration(Duration.ofSeconds(60 * 5), Duration.ofSeconds(1, 100_000_000));
        tester.test(metaData, DefaultSessionAccessMetaDataEntryMarshallerTestCase::assertEquals);
    }

    static void assertEquals(DefaultSessionAccessMetaDataEntry metaData1, DefaultSessionAccessMetaDataEntry metaData2) {
        Assert.assertEquals(metaData1.getSinceCreationDuration(), metaData2.getSinceCreationDuration());
        Assert.assertEquals(metaData1.getLastAccessDuration(), metaData2.getLastAccessDuration());
    }
}
