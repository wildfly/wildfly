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

package org.wildfly.clustering.web.infinispan;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EnumSet;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Externalizer;

import io.undertow.server.session.SecureRandomSessionIdGenerator;
import io.undertow.server.session.SessionIdGenerator;

/**
 * Unit test for {@link SessionIdentifierExternalizer}.
 *
 * @author Paul Ferraro
 */
public class SessionIdentifierExternalizerTestCase {
    @Test
    public void test() throws ClassNotFoundException, IOException {
        SessionIdGenerator generator = new SecureRandomSessionIdGenerator();

        for (Externalizer<String> externalizer : EnumSet.allOf(SessionIdentifierExternalizer.class)) {
            for (int i = 0; i < 100; ++i) {
                String id = generator.createSessionId();
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
                    externalizer.writeObject(output, id);
                }
                try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                    Assert.assertEquals(id, externalizer.readObject(input));
                }
            }
        }
    }
}
