/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling;

import java.io.IOException;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.Test;

/**
 * Generic tests for java.sql.* classes.
 * @author Paul Ferraro
 */
public abstract class AbstractSQLTestCase {

    private final MarshallingTesterFactory factory;

    public AbstractSQLTestCase(MarshallingTesterFactory factory) {
        this.factory = factory;
    }

    @Test
    public void testSQLDate() throws IOException {
        MarshallingTester<Date> tester = this.factory.createTester();
        tester.test(Date.valueOf(LocalDate.now()));
    }

    @Test
    public void testSQLTime() throws IOException {
        MarshallingTester<Time> tester = this.factory.createTester();
        tester.test(Time.valueOf(LocalTime.now()));
    }

    @Test
    public void testSQLTimestamp() throws IOException {
        MarshallingTester<Timestamp> tester = this.factory.createTester();
        tester.test(Timestamp.valueOf(LocalDateTime.now()));
    }
}
