/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

/**
 * A component's naming context operation mode.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public enum ComponentNamingMode {
    /**
     * No component namespace is available.
     */
    NONE,
    /**
     * Use the module's namespace for the component namespace.
     */
    USE_MODULE,
    /**
     * Create a new namespace for this component.
     */
    CREATE,
}
