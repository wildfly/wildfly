/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache;

import java.util.function.Function;

import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.marshalling.spi.FunctionalSerializer;
import org.wildfly.clustering.marshalling.spi.Serializer;

/**
 * Serializer for a key that delegates to the serializer of its identifier.
 * @author Paul Ferraro
 */
public class KeySerializer<K extends Key<I>, I> extends FunctionalSerializer<K, I> {

    public KeySerializer(Serializer<I> serializer, Function<I, K> factory) {
        super(serializer, Key::getId, factory);
    }
}
