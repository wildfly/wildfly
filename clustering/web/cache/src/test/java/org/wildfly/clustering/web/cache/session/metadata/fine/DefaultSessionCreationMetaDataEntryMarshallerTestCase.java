/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.MarshallingTester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for {@link SessionCreationMetaDataEntryExternalizer}.
 * @author Paul Ferraro
 */
public class DefaultSessionCreationMetaDataEntryMarshallerTestCase {

    @Test
    public void test() throws IOException {
        MarshallingTester<DefaultSessionCreationMetaDataEntry<Object>> tester = ProtoStreamTesterFactory.createTester(List.of(new FineSessionMetaDataSerializationContextInitializer()));

        DefaultSessionCreationMetaDataEntry<Object> entry = new DefaultSessionCreationMetaDataEntry<>(Instant.now());

        // Default max-inactive-interval
        entry.setTimeout(Duration.ofMinutes(30));
        tester.test(entry, DefaultSessionCreationMetaDataEntryMarshallerTestCase::assertEquals);

        // Custom max-inactive-interval
        entry.setTimeout(Duration.ofMinutes(10));
        tester.test(entry, DefaultSessionCreationMetaDataEntryMarshallerTestCase::assertEquals);
    }

    static void assertEquals(DefaultSessionCreationMetaDataEntry<Object> entry1, DefaultSessionCreationMetaDataEntry<Object> entry2) {
        // Compare only to millisecond precision
        Assert.assertEquals(entry1.getCreationTime().toEpochMilli(), entry2.getCreationTime().toEpochMilli());
        Assert.assertEquals(entry1.getTimeout(), entry2.getTimeout());
    }
}
