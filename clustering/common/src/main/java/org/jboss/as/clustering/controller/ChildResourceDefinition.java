/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Resource definition for child resources that performs all registration via {@link ManagementRegistrar#register(Object)}.
 * @author Paul Ferraro
 */
public abstract class ChildResourceDefinition<R extends ManagementResourceRegistration> extends AbstractResourceDefinition implements ChildResourceRegistrar<R> {

    protected ChildResourceDefinition(PathElement path, ResourceDescriptionResolver resolver) {
        super(new Parameters(path, resolver));
    }

    protected ChildResourceDefinition(Parameters parameters) {
        super(parameters);
    }
}
