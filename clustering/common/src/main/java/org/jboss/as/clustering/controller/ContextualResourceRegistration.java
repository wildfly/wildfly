/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.Optional;

import org.jboss.as.controller.services.path.PathManager;

/**
 * @author Paul Ferraro
 */
public class ContextualResourceRegistration extends DecoratingResourceRegistration<ManagementResourceRegistration> implements ManagementResourceRegistration {

    private final ManagementRegistrationContext context;

    public ContextualResourceRegistration(org.jboss.as.controller.registry.ManagementResourceRegistration registration, ManagementRegistrationContext context) {
        super(registration, r -> new ContextualResourceRegistration(r, context));
        this.context = context;
    }

    @Override
    public boolean isRuntimeOnlyRegistrationValid() {
        return this.context.isRuntimeOnlyRegistrationValid();
    }

    @Override
    public Optional<PathManager> getPathManager() {
        return this.context.getPathManager();
    }
}
