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

package org.wildfly.clustering.marshalling.protostream.util;

import java.util.BitSet;
import java.util.EnumSet;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamBuilder;

/**
 * @author Paul Ferraro
 */
public class EnumSetBuilder<E extends Enum<E>> implements ProtoStreamBuilder<EnumSet<E>> {

    private Class<E> enumClass = null;
    private boolean complement = false;
    private BitSet bits = new BitSet(0);

    public EnumSetBuilder<E> setEnumClass(Class<E> enumClass) {
        this.enumClass = enumClass;
        return this;
    }

    public EnumSetBuilder<E> setComplement(boolean complement) {
        this.complement = complement;
        return this;
    }

    public EnumSetBuilder<E> setBits(BitSet bits) {
        this.bits = bits;
        return this;
    }

    public Class<E> getEnumClass() {
        return this.enumClass;
    }

    @Override
    public EnumSet<E> build() {
        EnumSet<E> set = EnumSet.noneOf(this.enumClass);
        if (this.bits.size() > 0) {
            E[] enumValues = this.enumClass.getEnumConstants();
            for (int i = 0; i < enumValues.length; ++i) {
                if (this.bits.get(i)) {
                    set.add(enumValues[i]);
                }
            }
        }
        return this.complement ? EnumSet.complementOf(set) : set;
    }
}
