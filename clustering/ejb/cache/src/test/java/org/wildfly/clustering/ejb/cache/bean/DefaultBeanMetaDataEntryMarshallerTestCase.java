/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.UUIDSessionID;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.ee.cache.offset.Offset;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for {@link SimpleBeanEntryMarshaller}.
 * @author Paul Ferraro
 */
public class DefaultBeanMetaDataEntryMarshallerTestCase {

    @Test
    public void test() throws IOException {
        SessionID id = new UUIDSessionID(UUID.randomUUID());
        RemappableBeanMetaDataEntry<SessionID> metaData = new DefaultBeanMetaDataEntry<>("foo", id);
        Tester<RemappableBeanMetaDataEntry<SessionID>> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(metaData, DefaultBeanMetaDataEntryMarshallerTestCase::assertEquals);
        metaData.getLastAccess().setOffset(Offset.forInstant(Duration.ofSeconds(1)));
        tester.test(metaData, DefaultBeanMetaDataEntryMarshallerTestCase::assertEquals);
    }

    static void assertEquals(RemappableBeanMetaDataEntry<SessionID> entry1, RemappableBeanMetaDataEntry<SessionID> entry2) {
        Assert.assertEquals(entry1.getName(), entry2.getName());
        Assert.assertEquals(entry1.getGroupId(), entry2.getGroupId());
        Assert.assertEquals(entry1.getLastAccess().getBasis(), entry2.getLastAccess().getBasis());
        Assert.assertEquals(entry1.getLastAccess().get(), entry2.getLastAccess().get());
    }
}
