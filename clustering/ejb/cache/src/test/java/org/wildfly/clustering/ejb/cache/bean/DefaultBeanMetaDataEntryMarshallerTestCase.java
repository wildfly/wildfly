/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Duration;
import java.util.UUID;

import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.UUIDSessionID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;
import org.wildfly.clustering.server.offset.Offset;

/**
 * Unit test for {@link SimpleBeanEntryMarshaller}.
 * @author Paul Ferraro
 */
public class DefaultBeanMetaDataEntryMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        SessionID id = new UUIDSessionID(UUID.randomUUID());
        RemappableBeanMetaDataEntry<SessionID> metaData = new DefaultBeanMetaDataEntry<>("foo", id);
        Tester<RemappableBeanMetaDataEntry<SessionID>> tester = factory.createTester(DefaultBeanMetaDataEntryMarshallerTestCase::assertEquals);
        tester.accept(metaData);
        metaData.getLastAccessTime().setOffset(Offset.forInstant(Duration.ofSeconds(1)));
        tester.accept(metaData);
    }

    static void assertEquals(RemappableBeanMetaDataEntry<SessionID> entry1, RemappableBeanMetaDataEntry<SessionID> entry2) {
        Assertions.assertEquals(entry1.getName(), entry2.getName());
        Assertions.assertEquals(entry1.getGroupId(), entry2.getGroupId());
        Assertions.assertEquals(entry1.getLastAccessTime().getBasis(), entry2.getLastAccessTime().getBasis());
        Assertions.assertEquals(entry1.getLastAccessTime().get(), entry2.getLastAccessTime().get());
    }
}
