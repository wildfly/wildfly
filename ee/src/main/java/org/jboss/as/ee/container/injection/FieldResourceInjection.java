/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.container.injection;

import java.lang.reflect.Field;

import org.jboss.msc.inject.FieldInjector;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

/**
 * Resource injection capable of executing the resource injection using a Field instance.
 *
 * @param <V> The value type being injected
 *
 * @author John E. Bailey
 */
public class FieldResourceInjection<V> extends AbstractResourceInjection<V> {
    private final Value<Field> fieldValue;
    private Injector<V> injector;

    /**
     * Construct an instance.
     *
     * @param fieldValue The field on the target.
     * @param value The injection value
     * @param primitive Is the field type primitive
     */
    public FieldResourceInjection(final Value<Field> fieldValue, final Value<V> value, final boolean primitive) {
        super(value, primitive);
        this.fieldValue = fieldValue;
    }

    /** {@inheritDoc} */
    protected synchronized Injector<V> getInjector(final Object target) {
        if(injector == null) {
            injector = new FieldInjector<V>(Values.immediateValue(target), fieldValue);
        }
        return injector;
    }
}
