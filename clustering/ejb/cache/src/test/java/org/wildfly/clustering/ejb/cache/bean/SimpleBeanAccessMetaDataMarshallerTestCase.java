/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.io.IOException;
import java.time.Duration;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for {@link SimpleBeanAccessMetaDataMarshaller}.
 * @author Paul Ferraro
 */
public class SimpleBeanAccessMetaDataMarshallerTestCase {

    @Test
    public void test() throws IOException {
        BeanAccessMetaData metaData = new SimpleBeanAccessMetaData();
        Tester<BeanAccessMetaData> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(metaData, SimpleBeanAccessMetaDataMarshallerTestCase::assertEquals);
        metaData.setLastAccessDuration(Duration.ofSeconds(10));
        tester.test(metaData, SimpleBeanAccessMetaDataMarshallerTestCase::assertEquals);
    }

    static void assertEquals(BeanAccessMetaData metaData1, BeanAccessMetaData metaData2) {
        Assert.assertEquals(metaData1.getLastAccessDuration(), metaData2.getLastAccessDuration());
    }
}
