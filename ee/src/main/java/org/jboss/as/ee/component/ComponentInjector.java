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
package org.jboss.as.ee.component;


import org.jboss.msc.service.ServiceName;

/**
 * @author Stuart Douglas
 */
public interface ComponentInjector {

    /**
     * If this injector is a service then this will return the service name. Otherwise it will return null.
     *
     * The injected component should add this as a dependency to it's start task.
     *
     * @return The service name
     */
    ServiceName getServiceName();

    /**
     * Injects the component instance
     *
     * @param instance The component to inject
     * @return
     */
    InjectionHandle inject(Object instance);

    /**
     * A reference to a completed injection.
     */
    interface InjectionHandle {

        /**
         * Called at the end of the injected object lifecycle, to free any injected resources.
         */
        void uninject();
    }

}
