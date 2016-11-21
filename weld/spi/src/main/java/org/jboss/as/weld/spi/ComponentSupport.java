/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.weld.spi;

import javax.enterprise.inject.spi.InjectionTarget;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.weld.manager.api.WeldInjectionTarget;
import org.jboss.weld.manager.api.WeldManager;

/**
 * Java EE component support tools.
 *
 * @author Martin Kouba
 */
public interface ComponentSupport {

    /**
     * See also <a href="https://issues.jboss.org/browse/WFLY-4185">WFLY-4185</a>
     *
     * @param componentDescription
     * @return <code>false</code> if the specified component type should be discovered, <code>false</code> otherwise
     */
    default boolean isDiscoveredExternalType(ComponentDescription componentDescription) {
        return true;
    }

    /**
     * The first component suppor processing the given description is later allowed to {@link #processInjectionTarget(WeldInjectionTarget, ComponentDescription, WeldManager)}.
     *
     * @param componentDescription
     * @return
     */
    boolean isProcessing(ComponentDescription componentDescription);

    /**
     *
     * @param injectionTarget
     * @param componentDescription
     * @param beanManager
     * @return the processed injection target
     * @see #isProcessing(ComponentDescription)
     */
    default <T> InjectionTarget<T> processInjectionTarget(WeldInjectionTarget<T> injectionTarget, ComponentDescription componentDescription, WeldManager beanManager) {
        return beanManager.fireProcessInjectionTarget(injectionTarget.getAnnotatedType(), injectionTarget);
    }

}
