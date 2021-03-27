/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.IdentifierMarshaller;

import io.undertow.server.session.SecureRandomSessionIdGenerator;

/**
 * Unit test for {@link IdentifierMarshaller}.
 *
 * @author Paul Ferraro
 */
public class IdentifierSerializerTestCase {

    @Test
    public void testString() throws IOException {
        test(IdentifierMarshaller.ISO_LATIN_1, () -> UUID.randomUUID().toString());
    }

    @Test
    public void testBase64() throws IOException {
        io.undertow.server.session.SessionIdGenerator generator = new SecureRandomSessionIdGenerator();
        test(IdentifierMarshaller.BASE64, () -> generator.createSessionId());
    }

    @Test
    public void testHex() throws IOException {
        test(IdentifierMarshaller.HEX, () -> {
            // Adapted from org.apache.catalina.util.StandardSessionIdGenerator
            byte[] buffer = new byte[16];
            int sessionIdLength = 16;

            // Render the result as a String of hexadecimal digits
            StringBuilder builder = new StringBuilder(2 * sessionIdLength);

            int resultLenBytes = 0;

            Random random = new Random(System.currentTimeMillis());
            while (resultLenBytes < sessionIdLength) {
                random.nextBytes(buffer);
                for (int j = 0; j < buffer.length && resultLenBytes < sessionIdLength; j++) {
                    byte b1 = (byte) ((buffer[j] & 0xf0) >> 4);
                    byte b2 = (byte) (buffer[j] & 0x0f);
                    if (b1 < 10)
                        builder.append((char) ('0' + b1));
                    else
                        builder.append((char) ('A' + (b1 - 10)));
                    if (b2 < 10)
                        builder.append((char) ('0' + b2));
                    else
                        builder.append((char) ('A' + (b2 - 10)));
                    resultLenBytes++;
                }
            }
            return builder.toString();
        });
    }

    private static void test(Marshaller<String, ByteBuffer> marshaller, Supplier<String> generator) throws IOException {
        for (int i = 0; i < 100; ++i) {
            String id = generator.get();
            ByteBuffer buffer = marshaller.write(id);
            Assert.assertEquals(id, marshaller.read(buffer));
        }
    }
}
