/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.dmr.ModelNode;

/**
 * @author Paul Ferraro
 */
public interface OperationStepHandlerDescriptor {

    /**
     * The capabilities provided by this resource, paired with the condition under which they should be [un]registered
     * @return a map of capabilities to predicates
     */
    default Map<RuntimeCapability<?>, Predicate<ModelNode>> getCapabilities() {
        return Collections.emptyMap();
    }
}
