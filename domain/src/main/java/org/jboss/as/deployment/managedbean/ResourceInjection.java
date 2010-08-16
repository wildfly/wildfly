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

package org.jboss.as.deployment.managedbean;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;

/**
 * Helper object used to coordinate resource injection.  This class will hold onto an injector and an injected value
 * to apply at injection time.
 *
 * @author John E. Bailey
 */
public abstract class ResourceInjection <T> {
    private final InjectedValue<T> value = new InjectedValue<T>();
    private final boolean primitiveTarget;

    /**
     * Construct new instance.
     * @param primitiveTarget Is the injection target a primitive value
     */
    protected ResourceInjection(final boolean primitiveTarget) {
        this.primitiveTarget = primitiveTarget;
    }

    /**
     * Run the injection by passing the injected value into the injector.
     */
    public void inject(final Object target) {
        final Injector<T> injector = getInjector(target);
        final T theValue = value.getValue();
        if(primitiveTarget && theValue == null) {
            return; // Skip the injection of null into a primitive target
        }
        injector.inject(theValue);
    }

    /**
     * Get the value injector.
     *
     * @return An injector
     */
    public Injector<T> getValueInjector() {
        return value;
    }

    /**
     * Get the injector capable of performing the injection on the provided target.
     * 
     * @param target The target object of the injection
     * @return an injector
     */
    protected abstract Injector<T> getInjector(final Object target);
}
