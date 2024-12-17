/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import org.jboss.ejb.client.SessionID;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.ejb.cache.bean.BeanMetaDataKey;
import org.wildfly.clustering.ejb.cache.bean.RemappableBeanMetaDataEntry;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * @author Paul Ferraro
 */
public class InfinispanBeanMetaDataFilterTestCase {

    @ParameterizedTest
    @TesterFactorySource({ MarshallingTesterFactory.class })
    public void test(TesterFactory factory) {
        Tester<InfinispanBeanMetaDataFilter<BeanMetaDataKey<SessionID>, RemappableBeanMetaDataEntry<SessionID>>> tester = factory.createTester();
        tester.accept(new InfinispanBeanMetaDataFilter<>("foo"));
    }
}
