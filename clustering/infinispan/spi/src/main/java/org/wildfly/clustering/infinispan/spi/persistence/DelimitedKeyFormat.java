/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi.persistence;

import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * {@link KeyFormat} for keys with multiple string fields.
 * @author Paul Ferraro
 */
public class DelimitedKeyFormat<K> extends SimpleKeyFormat<K> {

    public DelimitedKeyFormat(Class<K> targetClass, String delimiter, Function<String[], K> parser, Function<K, String[]> formatter) {
        super(targetClass, value -> parser.apply(value.split(Pattern.quote(delimiter))), key -> String.join(delimiter, formatter.apply(key)));
    }
}
