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

package org.wildfly.clustering.marshalling.protostream;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class SimpleObjectInputTestCase {

    @Test
    public void test() throws IOException, ClassNotFoundException {
        final Object[] objects = new Object[] { UUID.randomUUID(), UUID.randomUUID() };
        final String[] strings = new String[] { "foo", "bar" };
        final int[] ints = new int[] { Integer.MIN_VALUE, Integer.MAX_VALUE };
        final long[] longs = new long[] { Long.MIN_VALUE, Long.MAX_VALUE };
        final double[] doubles = new double[] { Double.MIN_VALUE, Double.MAX_VALUE };

        ObjectInput input = new SimpleObjectInput.Builder().with(objects).with(strings).with(ints).with(longs).with(doubles).build();

        TestExternalizable test = new TestExternalizable();
        test.readExternal(input);

        for (int i = 0; i < 2; ++i) {
            Assert.assertSame(objects[i], test.objects[i]);
            Assert.assertSame(strings[i], test.strings[i]);
            Assert.assertEquals(ints[i], test.ints[i]);
            Assert.assertEquals(longs[i], test.longs[i]);
            Assert.assertEquals(doubles[i], test.doubles[i], 0);
        }
    }

    static class TestExternalizable implements Externalizable {

        final Object[] objects = new Object[2];
        final String[] strings = new String[2];
        final ByteBuffer[] buffers = new ByteBuffer[2];
        final int[] ints = new int[2];
        final long[] longs = new long[2];
        final double[] doubles = new double[2];

        @Override
        public void writeExternal(ObjectOutput output) {
        }

        @Override
        public void readExternal(ObjectInput input) throws ClassNotFoundException, IOException {
            for (int i = 0; i < 2; ++i) {
                this.objects[i] = input.readObject();
                this.strings[i] = input.readUTF();
                this.ints[i] = input.readInt();
                this.longs[i] = input.readLong();
                this.doubles[i] = input.readDouble();
            }
        }
    }
}
