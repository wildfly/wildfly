/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.config;

/**
 * Possible choices for how extended persistence context inheritance is performed.
 *
 * @author Scott Marlow
 */
public enum ExtendedPersistenceInheritance {
    DEEP,       // extended persistence context can be inherited from sibling beans as well as a parent (or
                // recursively parents of parent) bean.
                // the parent can be the injecting bean (creation time inheritance) or from the bean call stack.
                // JNDI lookup of a bean, also qualifies for inheritance

    SHALLOW     // extended persistence context can only be inherited from a single level (immediate) parent bean.
                // the parent can be the injecting bean (creation time inheritance) or from the bean call stack.
}
