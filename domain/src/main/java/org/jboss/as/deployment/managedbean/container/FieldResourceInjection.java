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

package org.jboss.as.deployment.managedbean.container;

import org.jboss.msc.inject.FieldInjector;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

import java.lang.reflect.Field;

/**
 * Resource injection capable of executing the resource injection using a Field instance.
 *
 * @author John E. Bailey
 */
public class FieldResourceInjection<T> extends ResourceInjection<T> {
    private final Value<Field> fieldValue;

    /**
     * Construct an instance.
     *
     * @param fieldValue The field on the target.
     * @param primitive Is the field type primitive
     */
    public FieldResourceInjection(final Value<Field> fieldValue, final boolean primitive) {
        super(primitive);
        this.fieldValue = fieldValue;
    }

    /** {@inheritDoc} */
    protected Injector<T> getInjector(final Object target) {
        return new FieldInjector<T>(Values.immediateValue(target), fieldValue);  
    }
}
