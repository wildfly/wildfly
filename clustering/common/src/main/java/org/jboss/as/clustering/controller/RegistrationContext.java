/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import java.util.Optional;

import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.services.path.PathManager;

/**
 * Context used for conditional registration.
 * @author Paul Ferraro
 */
public interface RegistrationContext {
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
