/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.Optional;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.services.path.PathManager;

/**
 * @author Paul Ferraro
 */
public class ExtensionRegistrationContext implements ManagementRegistrationContext {

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
