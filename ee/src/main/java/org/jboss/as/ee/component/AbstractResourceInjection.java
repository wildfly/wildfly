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

package org.jboss.as.ee.component;

import org.jboss.as.naming.JndiInjectable;
import org.jboss.msc.value.Value;

/**
 * This class will hold onto an injector and an injected value to apply them to the target at injection time.
 *
 * @author John E. Bailey
 */
public abstract class AbstractResourceInjection implements ResourceInjection {

    /**
     * Flag specifying whether this injection target is primitive.
     */
    protected final boolean primitiveTarget;
    private final Value<JndiInjectable> value;

    /**
     * Construct new instance.
     *
     * @param value The value to inject
     * @param primitiveTarget Is the injection target a primitive value
     */
    protected AbstractResourceInjection(final Value<JndiInjectable> value, final boolean primitiveTarget) {
        this.value = value;
        this.primitiveTarget = primitiveTarget;
    }

    /** {@inheritDoc} **/
    public void inject(final Object target) {
        final Object value = this.value.getValue().getInjectedValue();
        if(primitiveTarget && value == null) {
            return;
        }
        doInject(target, value);
    }

    /**
     * Do the injection.
     *
     * @param target the target instance on to which to inject ({@code null} if it is static)
     * @param value the value to inject
     */
    protected abstract void doInject(final Object target, final Object value);
}
