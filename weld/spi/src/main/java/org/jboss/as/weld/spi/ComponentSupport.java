/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.spi;

import jakarta.enterprise.inject.spi.InjectionTarget;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.weld.manager.api.WeldInjectionTarget;
import org.jboss.weld.manager.api.WeldManager;

/**
 * Jakarta EE component support tools.
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
