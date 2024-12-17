/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.stream.Stream;

import org.jboss.as.clustering.controller.SubsystemResourceDescription;
import org.jboss.as.controller.AttributeDefinition;

/**
 * @author Paul Ferraro
 */
public enum InfinispanSubsystemResourceDescription implements SubsystemResourceDescription {
    INSTANCE;

    @Override
    public String getName() {
        return "infinispan";
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.empty();
    }
}
