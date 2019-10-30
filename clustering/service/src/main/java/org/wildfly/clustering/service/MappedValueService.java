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

package org.wildfly.clustering.service;

import java.util.function.Function;

import org.jboss.msc.value.Value;

/**
 * A generic {@link org.jboss.msc.service.Service} whose value is the result of applying a mapping function to a provided {@link Value}.
 * @author Paul Ferraro
 * @deprecated Replaced by {@link FunctionalService}.
 */
@Deprecated
public class MappedValueService<T, V> extends FunctionalValueService<T, V> {

    /**
     * Constructs a new mapped value service.
     * @param mapper a function that maps the supplied value to the service value
     * @param supplier produces the supplied value
     */
    public MappedValueService(Function<T, V> mapper, Value<T> supplier) {
        super(mapper, context -> supplier.getValue(), (context, value) -> {});
    }
}
