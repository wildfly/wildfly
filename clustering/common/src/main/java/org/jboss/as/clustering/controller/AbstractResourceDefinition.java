/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Resource definition for resources that performs all registration via {@link ManagementRegistrar#register(Object)}.
 * All other registerXXX(...) methods have been made final - to prevent misuse.
 * @author Paul Ferraro
 */
public abstract class AbstractResourceDefinition extends SimpleResourceDefinition {

    protected AbstractResourceDefinition(PathElement path, ResourceDescriptionResolver resolver) {
        super(new Parameters(path, resolver));
    }

    protected AbstractResourceDefinition(Parameters parameters) {
        super(parameters);
    }

    @Override
    public final void registerOperations(ManagementResourceRegistration resourceRegistration) {
    }

    @Override
    public final void registerAttributes(ManagementResourceRegistration resourceRegistration) {
    }

    @Override
    public final void registerNotifications(ManagementResourceRegistration resourceRegistration) {
    }

    @Override
    public final void registerChildren(ManagementResourceRegistration resourceRegistration) {
    }

    @Override
    public final void registerCapabilities(ManagementResourceRegistration resourceRegistration) {
    }
}
