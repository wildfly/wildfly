/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.ejb.timer.TimeoutMetaData;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * @author Paul Ferraro
 */
public class SimpleTimeoutMetaDataTestCase {

    @ParameterizedTest
    @TesterFactorySource({ MarshallingTesterFactory.class })
    public void test(TesterFactory factory) {
        Tester<TimeoutMetaData> tester = factory.createTester((expected, actual) -> assertThat(actual.getNextTimeout()).isEqualTo(expected.getNextTimeout()));
        tester.accept(new SimpleTimeoutMetaData(Optional.empty()));
        tester.accept(new SimpleTimeoutMetaData(Optional.of(Instant.now().truncatedTo(ChronoUnit.MILLIS))));
    }
}
