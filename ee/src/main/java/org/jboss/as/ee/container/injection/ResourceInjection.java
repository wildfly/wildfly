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
import java.lang.reflect.Method;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

/**
 * An instance of an injection.  This can be called at any time against an object instance to actually perform the
 * injection.
 *
 * @author John Bailey
 */
public interface ResourceInjection {
    /**
     * Run this resource injection on the target instance.
     *
     * @param target The target object to inject
     */
    void inject(final Object target );


    /**
     * Factory to create injections
     */
    class Factory {
        /**
         * Create the correct injection instance.
         *
         * @param resourceConfiguration The resource injection configuration
         * @param value The value for injection
         * @param <V> The value type
         * @return The injection instance
         */
        public static <V> ResourceInjection create(final ResourceInjectionConfiguration resourceConfiguration, Value<V> value) {
            switch(resourceConfiguration.getTargetType()) {
                case FIELD:
                    return new FieldResourceInjection<V>(Values.immediateValue(Field.class.cast(resourceConfiguration.getTarget())), value, resourceConfiguration.getInjectedType().isPrimitive());
                case METHOD:
                    return new MethodResourceInjection<V>(Values.immediateValue(Method.class.cast(resourceConfiguration.getTarget())), value, resourceConfiguration.getInjectedType().isPrimitive());
                default:
                    return null;
            }
        }
    }
}
