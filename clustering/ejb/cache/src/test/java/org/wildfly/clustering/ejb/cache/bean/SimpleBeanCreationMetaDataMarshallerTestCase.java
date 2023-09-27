/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.io.IOException;
import java.util.UUID;

import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.UUIDSessionID;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for {@link SimpleBeanEntryMarshaller}.
 * @author Paul Ferraro
 */
public class SimpleBeanCreationMetaDataMarshallerTestCase {

    @Test
    public void test() throws IOException {
        SessionID id = new UUIDSessionID(UUID.randomUUID());
        BeanCreationMetaData<SessionID> metaData = new SimpleBeanCreationMetaData<>("foo", id);
        Tester<BeanCreationMetaData<SessionID>> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(metaData, SimpleBeanCreationMetaDataMarshallerTestCase::assertEquals);
    }

    static void assertEquals(BeanCreationMetaData<SessionID> metaData1, BeanCreationMetaData<SessionID> metaData2) {
        Assert.assertEquals(metaData1.getName(), metaData2.getName());
        Assert.assertEquals(metaData1.getGroupId(), metaData2.getGroupId());
        Assert.assertEquals(metaData1.getCreationTime(), metaData2.getCreationTime());
    }
}
