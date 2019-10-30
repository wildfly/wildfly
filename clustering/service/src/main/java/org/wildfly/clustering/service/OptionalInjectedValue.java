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

import java.util.Optional;

import org.jboss.msc.inject.RetainingInjector;
import org.jboss.msc.value.Value;

/**
 * Like {@link org.jboss.msc.value.InjectedValue}, but with support for returning an {@link Optional}.
 * @author Paul Ferraro
 * @deprecated Replaced by a {@link java.util.function.Supplier} that can return null.
 */
@Deprecated
public class OptionalInjectedValue<T> extends RetainingInjector<T> implements Value<T> {

    @Override
    public T getValue() {
        Value<T> value = getStoredValue();
        if (value == null) {
            throw new IllegalStateException();
        }
        return value.getValue();
    }

    /**
     * Returns the optional value, which is only defined if a value was injected.
     * Analogous to {@link org.jboss.msc.value.InjectedValue#getOptionalValue()}.
     * @return an optional injected value
     */
    public Optional<T> getOptionalValue() {
        return Optional.ofNullable(this.getStoredValue()).map(Value::getValue);
    }
}
