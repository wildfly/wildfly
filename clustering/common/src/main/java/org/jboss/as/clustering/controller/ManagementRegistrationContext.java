/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.Optional;

import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.services.path.PathManager;

/**
 * Context used for conditional registration.
 * @author Paul Ferraro
 */
public interface ManagementRegistrationContext {
    /**
     * Gets whether it is valid for the extension to register resources, attributes or operations that do not
     * involve the persistent configuration, but rather only involve runtime services. Extensions should use this
     * method before registering such "runtime only" resources, attributes or operations. This
     * method is intended to avoid registering resources, attributes or operations on process types that
     * can not install runtime services.
     *
     * @return whether it is valid to register runtime resources, attributes, or operations.
     * @see org.jboss.as.controller.ExtensionContext#isRuntimeOnlyRegistrationValid()
     */
    boolean isRuntimeOnlyRegistrationValid();

    /**
     * Returns the optional {@link PathManager} of the process that is only present if the process is a {@link ProcessType#isServer() server}.
     *
     * @return an optional PathManager.
     * @see org.jboss.as.controller.ExtensionContext#getPathManager()
     */
    Optional<PathManager> getPathManager();
}
