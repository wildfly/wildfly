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

package org.wildfly.clustering.ee.infinispan.expiration;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.ee.expiration.ExpirationMetaData;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Marshaller test for {@link SimpleExpirationMetaData}.
 * @author Paul Ferraro
 */
public class SimpleExpirationMetaDataMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<ExpirationMetaData> tester = ProtoStreamTesterFactory.INSTANCE.createTester();

        tester.test(new SimpleExpirationMetaData(Duration.ofMinutes(30), Instant.EPOCH), this::assertEquals);
        tester.test(new SimpleExpirationMetaData(Duration.ofSeconds(600), Instant.now()), this::assertEquals);
    }

    private void assertEquals(ExpirationMetaData expected, ExpirationMetaData actual) {
        Assert.assertEquals(expected.getTimeout(), actual.getTimeout());
        Assert.assertEquals(expected.getLastAccessTime(), actual.getLastAccessTime());
    }
}
