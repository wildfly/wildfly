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

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.services.path.PathManager;

/**
 * @author Paul Ferraro
 */
public class ExtensionRegistrationContext implements RegistrationContext {

    private final boolean runtimeOnlyRegistrationValid;
    private final Optional<PathManager> pathManager;

    public ExtensionRegistrationContext(ExtensionContext context) {
        this.runtimeOnlyRegistrationValid = context.isRuntimeOnlyRegistrationValid();
        this.pathManager = context.getProcessType().isServer() ? Optional.of(context.getPathManager()) : Optional.empty();
    }

    @Override
    public boolean isRuntimeOnlyRegistrationValid() {
        return this.runtimeOnlyRegistrationValid;
    }

    @Override
    public Optional<PathManager> getPathManager() {
        return this.pathManager;
    }
}
