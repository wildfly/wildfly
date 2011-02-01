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

package org.jboss.as.ee.component.injection;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceName;

/**
 * Holder object for the information required to establish a proper resolved dependency.
 *
 * @param <T> The injection type type
 * @author John Bailey
 */
public class ResourceInjectionDependency<T> {
    private final ServiceName dependencyName;
    private final Injector<T> injector;
    private final Class<T> injectionType;

    public ResourceInjectionDependency(final ServiceName dependencyName) {
        this(dependencyName, null, null);
    }

    public ResourceInjectionDependency(final ServiceName dependencyName, final Class<T> injectionType, final Injector<T> injector) {
        this.dependencyName = dependencyName;
        this.injector = injector;
        this.injectionType = injectionType;
    }

    /**
     * The service name for the dependency
     *
     * @return The dependency name
     */
    public ServiceName getServiceName() {
        return dependencyName;
    }

    /**
     * The injector (if available) for this dependency
     *
     * @return The injector
     */
    public Injector<T> getInjector() {
        return injector;
    }

    /**
     * The injection type (if the injector is available) of this dependency.
     *
     * @return The injection type
     */
    public Class<T> getInjectorType() {
        return injectionType;
    }
}
