/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
