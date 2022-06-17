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
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class SimpleObjectOutputTestCase {

    @Test
    public void test() throws IOException {
        Object[] objects = new Object[2];
        String[] strings = new String[2];
        int[] ints = new int[2];
        long[] longs = new long[2];
        double[] doubles = new double[2];

        ObjectOutput output = new SimpleObjectOutput.Builder().with(objects).with(strings).with(ints).with(longs).with(doubles).build();

        TestExternalizable test = new TestExternalizable();
        test.writeExternal(output);

        for (int i = 0; i < 2; ++i) {
            Assert.assertSame(test.objects[i], objects[i]);
            Assert.assertSame(test.strings[i], strings[i]);
            Assert.assertEquals(test.ints[i], ints[i]);
            Assert.assertEquals(test.longs[i], longs[i]);
            Assert.assertEquals(test.doubles[i], doubles[i], 0);
        }
    }

    static class TestExternalizable implements Externalizable {

        final Object[] objects = new Object[] { UUID.randomUUID(), UUID.randomUUID() };
        final String[] strings = new String[] { "foo", "bar" };
        final int[] ints = new int[] { Integer.MIN_VALUE, Integer.MAX_VALUE };
        final long[] longs = new long[] { Long.MIN_VALUE, Long.MAX_VALUE };
        final double[] doubles = new double[] { Double.MIN_VALUE, Double.MAX_VALUE };

        @Override
        public void writeExternal(ObjectOutput output) throws IOException {
            for (int i = 0; i < 2; ++i) {
                output.writeObject(this.objects[i]);
                output.writeUTF(this.strings[i]);
                output.writeInt(this.ints[i]);
                output.writeLong(this.longs[i]);
                output.writeDouble(this.doubles[i]);
            }
        }

        @Override
        public void readExternal(ObjectInput input) {
        }
    }
}
