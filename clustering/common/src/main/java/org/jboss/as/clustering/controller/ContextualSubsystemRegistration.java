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
public class ContextualSubsystemRegistration extends DecoratingSubsystemRegistration<ManagementResourceRegistration> implements SubsystemRegistration {

    private final ManagementRegistrationContext context;

    public ContextualSubsystemRegistration(org.jboss.as.controller.SubsystemRegistration registration, ExtensionContext context) {
        this(registration, new ExtensionRegistrationContext(context));
    }

    public ContextualSubsystemRegistration(org.jboss.as.controller.SubsystemRegistration registration, ManagementRegistrationContext context) {
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
