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

/**
 * InjectionFactories provide additional injections after an EE component has been created.
 * <p/>
 * The main use case for this is performing CDI injection on EE components.
 *
 * @author Stuart Douglas
 */
public interface InjectionFactory {

    /**
     * Creates a component injector for the given component. If the component does not contain any injection
     * points that this injector understands then null is returned.
     *
     * @param component The component to generate an injector for
     * @return an injector for the component, or null if there are no injection points
     */
    ComponentInjector createInjector(AbstractComponentConfiguration component);

}
