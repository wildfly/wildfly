/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.naming;

import java.util.function.Supplier;

import org.jboss.msc.value.Value;

/**
 * A ManagedReference that simply holds a value.
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ValueManagedReference implements ManagedReference {
    private final Supplier<?> value;

    /**
     * Construct a new instance.
     *
     * @param value the value to wrap
     * @deprecated use {@link ValueManagedReference#ValueManagedReference(Object)} instead. This constructor will be removed in the future.
     */
    @Deprecated
    public ValueManagedReference(final Value<?> value) {
        this.value = () -> value.getValue();
    }

    /**
     * Construct a new instance.
     *
     * @param value the value to wrap
     */
    public ValueManagedReference(final Object value) {
        this.value = () -> value;
    }

    @Override
    public void release() {

    }

    @Override
    public Object getInstance() {
        return value.get();
    }
}
