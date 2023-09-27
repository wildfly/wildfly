/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.util.function.Function;

/**
 * {@link Formatter} for keys with a simple string representation.
 * @author Paul Ferraro
 */
public class SimpleFormatter<K> implements Formatter<K> {

    private final Class<K> targetClass;
    private final Function<String, K> parser;
    private final Function<K, String> formatter;

    public SimpleFormatter(Class<K> targetClass, Function<String, K> parser) {
        this(targetClass, parser, Object::toString);
    }

    public SimpleFormatter(Class<K> targetClass, Function<String, K> parser, Function<K, String> formatter) {
        this.targetClass = targetClass;
        this.parser = parser;
        this.formatter = formatter;
    }

    @Override
    public Class<K> getTargetClass() {
        return this.targetClass;
    }

    @Override
    public K parse(String value) {
        return this.parser.apply(value);
    }

    @Override
    public String format(K key) {
        return this.formatter.apply(key);
    }
}
