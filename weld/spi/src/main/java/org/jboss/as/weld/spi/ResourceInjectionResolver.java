/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.spi;

/**
 *
 * @author Martin Kouba
 */
public interface ResourceInjectionResolver {

    /**
     *
     * @param resourceName
     * @return the resolved object or <code>null</code> if not able to resolve the given name
     */
    Object resolve(String resourceName);

}
