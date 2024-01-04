/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;

/**
 * Resource definition for subsystem resources that performs all registration via {@link #register(SubsystemRegistration)}.
 * @author Paul Ferraro
 */
public abstract class SubsystemResourceDefinition extends AbstractResourceDefinition implements ManagementRegistrar<SubsystemRegistration> {

    protected static PathElement pathElement(String name) {
        return PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, name);
    }

    protected SubsystemResourceDefinition(PathElement path, ResourceDescriptionResolver resolver) {
        super(path, resolver);
    }
}
