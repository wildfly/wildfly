/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.cache.session;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.MarshallingTester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for {@link SessionCreationMetaDataEntryExternalizer}.
 * @author Paul Ferraro
 */
public class SessionCreationMetaDataEntryMarshallerTestCase {

    @Test
    public void test() throws IOException {
        MarshallingTester<SessionCreationMetaDataEntry<Object>> tester = new ProtoStreamTesterFactory(SessionCreationMetaDataEntry.class.getClassLoader()).createTester();

        SessionCreationMetaData metaData = new SimpleSessionCreationMetaData(Instant.now());
        SessionCreationMetaDataEntry<Object> entry = new SessionCreationMetaDataEntry<>(metaData);

        // Default max-inactive-interval
        metaData.setMaxInactiveInterval(Duration.ofMinutes(30));
        tester.test(entry, SessionCreationMetaDataEntryMarshallerTestCase::assertEquals);

        // Custom max-inactive-interval
        metaData.setMaxInactiveInterval(Duration.ofMinutes(10));
        tester.test(entry, SessionCreationMetaDataEntryMarshallerTestCase::assertEquals);
    }

    static void assertEquals(SessionCreationMetaDataEntry<Object> entry1, SessionCreationMetaDataEntry<Object> entry2) {
        // Compare only to millisecond precision
        Assert.assertEquals(entry1.getMetaData().getCreationTime().toEpochMilli(), entry2.getMetaData().getCreationTime().toEpochMilli());
        Assert.assertEquals(entry1.getMetaData().getMaxInactiveInterval(), entry2.getMetaData().getMaxInactiveInterval());
    }
}
