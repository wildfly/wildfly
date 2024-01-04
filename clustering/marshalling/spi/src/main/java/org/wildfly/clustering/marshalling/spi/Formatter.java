/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

/**
 * Formats a cache key to a string representation and back again.
 * @author Paul Ferraro
 */
public interface Formatter<K> {
    /**
     * The implementation class of the target key of this format.
     * @return an implementation class
     */
    Class<K> getTargetClass();

    /**
     * Parses the key from the specified string.
     * @param value a string representation of the key
     * @return the parsed key
     */
    K parse(String value);

    /**
     * Formats the specified key to a string representation.
     * @param key a key to format
     * @return a string representation of the specified key.
     */
    String format(K key);
}
