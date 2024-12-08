/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller;

import java.util.stream.Stream;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;

/**
 * Describes a resource.
 * @author Paul Ferraro
 */
public interface ResourceDescription extends ResourceRegistration {

    default PathElement getPathKey() {
        return this.getPathElement();
    }

    default Stream<AttributeDefinition> getAttributes() {
        return Stream.empty();
    }
}
