/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.function.Supplier;

/**
 * Builder for an {@link EnumSet}.
 * @author Paul Ferraro
 * @param <E> the enum type of this builder
 */
public interface EnumSetBuilder<E extends Enum<E>> extends Supplier<EnumSet<E>> {

    EnumSetBuilder<E> setEnumClass(Class<E> enumClass);

    EnumSetBuilder<E> setComplement(boolean complement);

    EnumSetBuilder<E> setBits(BitSet bits);

    EnumSetBuilder<E> add(int ordinal);

    Class<E> getEnumClass();
}
