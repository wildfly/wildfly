/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.jboss.msc.service.ServiceName;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.cache.infinispan.embedded.persistence.FormatterTesterFactory;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Unit test for {@link ServiceNameFormatter}.
 * @author Paul Ferraro
 */
public class ServiceNameMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource({ MarshallingTesterFactory.class, FormatterTesterFactory.class })
    public void test(TesterFactory factory) {
        factory.createKeyTester().accept(ServiceName.JBOSS.append("foo", "bar"));
    }
}
