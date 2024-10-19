/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

/**
 * @author Paul Ferraro
 */
public interface SubsystemResourceDescription extends ResourceDescription {

    String getName();

    @Override
    default PathElement getPathElement() {
        return PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, this.getName());
    }
}
