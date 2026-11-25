/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.cache.infinispan.embedded.persistence.FormatterTesterFactory;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * @author Paul Ferraro
 */
public class InfinispanTimerMetaDataKeyTestCase {

    @ParameterizedTest
    @TesterFactorySource({ MarshallingTesterFactory.class, FormatterTesterFactory.class })
    public void test(TesterFactory factory) {
        Tester<InfinispanTimerMetaDataKey<UUID>> tester = factory.createKeyTester();
        tester.accept(new InfinispanTimerMetaDataKey<>(UUID.randomUUID()));
    }
}
