/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.stream.Stream;

import org.jboss.as.clustering.controller.DurationAttributeDefinition;
import org.jboss.as.clustering.controller.ResourceDescription;
import org.jboss.as.controller.AttributeDefinition;

/**
 * @author Paul Ferraro
 *
 */
public interface ScheduledThreadPoolResourceDescription extends ResourceDescription {
    AttributeDefinition getMinThreads();
    DurationAttributeDefinition getKeepAlive();

    @Override
    default Stream<AttributeDefinition> getAttributes() {
        return Stream.of(this.getMinThreads(), this.getKeepAlive());
    }
}
