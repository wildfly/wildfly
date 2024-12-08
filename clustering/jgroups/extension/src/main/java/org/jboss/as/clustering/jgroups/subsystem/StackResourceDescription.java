/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.stream.Stream;

import org.jboss.as.clustering.controller.ResourceDescription;
import org.jboss.as.clustering.controller.StatisticsEnabledAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;

/**
 * @author Paul Ferraro
 *
 */
public enum StackResourceDescription implements ResourceDescription {
    INSTANCE;

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("stack", name);
    }

    static final StatisticsEnabledAttributeDefinition STATISTICS_ENABLED = new StatisticsEnabledAttributeDefinition.Builder().build();

    private final PathElement path = pathElement(PathElement.WILDCARD_VALUE);

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.of(STATISTICS_ENABLED);
    }
}
