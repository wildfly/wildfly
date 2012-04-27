/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.web.impl;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

import org.jboss.as.clustering.MarshallingContext;
import org.jboss.as.clustering.VersionedMarshallingConfiguration;
import org.jboss.as.clustering.web.SessionAttributeMarshaller;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class SessionAttributeMarshallerTest implements VersionedMarshallingConfiguration {
    private final SessionAttributeMarshaller marshaller = new SessionAttributeMarshallerImpl(new MarshallingContext(Marshalling.getMarshallerFactory("river", Marshalling.class.getClassLoader()), this));

    @Override
    public int getCurrentMarshallingVersion() {
        return 0;
    }

    @Override
    public MarshallingConfiguration getMarshallingConfiguration(int version) {
        return new MarshallingConfiguration();
    }

    @Test
    public void test() throws IOException, ClassNotFoundException {
        this.test((Serializable) null, true);
        this.test("test", true);
        this.test(Boolean.TRUE, true);
        this.test(Byte.valueOf(Byte.MAX_VALUE), true);
        this.test(Character.valueOf(Character.MAX_VALUE), true);
        this.test(Double.valueOf(Double.MAX_VALUE), true);
        this.test(Float.valueOf(Float.MAX_VALUE), true);
        this.test(Integer.valueOf(Integer.MAX_VALUE), true);
        this.test(Long.valueOf(Long.MAX_VALUE), true);
        this.test(Short.valueOf(Short.MAX_VALUE), true);
        this.test(new String[] { "test" }, true);
        this.test(new boolean[] { Boolean.TRUE }, true);
        this.test(new byte[] { Byte.MAX_VALUE }, true);
        this.test(new char[] { Character.MAX_VALUE }, true);
        this.test(new double[] { Double.MAX_VALUE }, true);
        this.test(new float[] { Float.MAX_VALUE }, true);
        this.test(new int[] { Integer.MAX_VALUE }, true);
        this.test(new long[] { Long.MAX_VALUE }, true);
        this.test(new short[] { Short.MAX_VALUE }, true);
        this.test(new Boolean[] { Boolean.TRUE }, true);
        this.test(new Byte[] { Byte.valueOf(Byte.MAX_VALUE) }, true);
        this.test(new Character[] { Character.valueOf(Character.MAX_VALUE) }, true);
        this.test(new Double[] { Double.valueOf(Double.MAX_VALUE) }, true);
        this.test(new Float[] { Float.valueOf(Float.MAX_VALUE) }, true);
        this.test(new Integer[] { Integer.valueOf(Integer.MAX_VALUE) }, true);
        this.test(new Long[] { Long.valueOf(Long.MAX_VALUE) }, true);
        this.test(new Short[] { Short.valueOf(Short.MAX_VALUE) }, true);
        this.test(this.getClass(), false);
        this.test(new Date(System.currentTimeMillis()), false);
        this.test(new Object(), false);
    }

    private void test(Object original, boolean same) throws IOException, ClassNotFoundException {
        try {
            Object marshalled = this.marshaller.marshal(original);

            if (original != null) {
                assertTrue(original instanceof Serializable);
            }
            if (marshalled != null) {
                assertTrue(marshalled instanceof Serializable);
            }

            if (same) {
                assertSame(original, marshalled);
            } else {
                assertNotSame(original, marshalled);
            }

            Object unmarshalled = this.marshaller.unmarshal(marshalled);

            if (same) {
                assertSame(marshalled, unmarshalled);
            } else {
                assertNotSame(marshalled, unmarshalled);
                assertEquals(original, unmarshalled);
            }
        } catch (IllegalArgumentException e) {
            assertFalse(same);
            assertFalse(original instanceof Serializable);
        }
    }
}
