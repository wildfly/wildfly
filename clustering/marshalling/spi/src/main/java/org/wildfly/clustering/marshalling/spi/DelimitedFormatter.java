/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * {@link Formatter} for keys with multiple string fields.
 * @author Paul Ferraro
 */
public class DelimitedFormatter<K> extends SimpleFormatter<K> {

    public DelimitedFormatter(Class<K> targetClass, String delimiter, Function<String[], K> parser, Function<K, String[]> formatter) {
        super(targetClass, value -> parser.apply(value.split(Pattern.quote(delimiter))), key -> String.join(delimiter, formatter.apply(key)));
    }
}
