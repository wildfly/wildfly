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

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.MarshallingTester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for {@link SessionAccessMetaDataExternalizer}.
 * @author Paul Ferraro
 */
public class SessionAccessMetaDataMarshallerTestCase {

    @Test
    public void test() throws IOException {
        MarshallingTester<SimpleSessionAccessMetaData> tester = new ProtoStreamTesterFactory(SimpleSessionAccessMetaData.class.getClassLoader()).createTester();

        SimpleSessionAccessMetaData metaData = new SimpleSessionAccessMetaData();

        // New session
        metaData.setLastAccessDuration(Duration.ZERO, Duration.ofNanos(100_000_000));
        tester.test(metaData, SessionAccessMetaDataMarshallerTestCase::assertEquals);

        // Existing session, sub-second response time
        metaData.setLastAccessDuration(Duration.ofSeconds(60 * 5), Duration.ofNanos(100_000_000));
        tester.test(metaData, SessionAccessMetaDataMarshallerTestCase::assertEquals);

        // Existing session, +1 second response time
        metaData.setLastAccessDuration(Duration.ofSeconds(60 * 5), Duration.ofSeconds(1, 100_000_000));
        tester.test(metaData, SessionAccessMetaDataMarshallerTestCase::assertEquals);
    }

    static void assertEquals(SimpleSessionAccessMetaData metaData1, SimpleSessionAccessMetaData metaData2) {
        Assert.assertEquals(metaData1.getSinceCreationDuration(), metaData2.getSinceCreationDuration());
        Assert.assertEquals(metaData1.getLastAccessDuration(), metaData2.getLastAccessDuration());
    }
}
