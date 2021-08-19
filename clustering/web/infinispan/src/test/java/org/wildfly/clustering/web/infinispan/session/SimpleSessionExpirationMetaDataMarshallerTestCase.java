/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.infinispan.session;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.web.session.SessionExpirationMetaData;

/**
 * Marshaller test for {@link SimpleSessionExpirationMetaData}.
 * @author Paul Ferraro
 */
public class SimpleSessionExpirationMetaDataMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<SessionExpirationMetaData> tester = ProtoStreamTesterFactory.INSTANCE.createTester();

        tester.test(new SimpleSessionExpirationMetaData(Duration.ofMinutes(30), Instant.EPOCH), this::assertEquals);
        tester.test(new SimpleSessionExpirationMetaData(Duration.ofSeconds(600), Instant.now()), this::assertEquals);
    }

    private void assertEquals(SessionExpirationMetaData expected, SessionExpirationMetaData actual) {
        Assert.assertEquals(expected.getMaxInactiveInterval(), actual.getMaxInactiveInterval());
        Assert.assertEquals(expected.getLastAccessEndTime(), actual.getLastAccessEndTime());
    }
}
