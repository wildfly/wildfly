/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.UnaryOperator;

import org.infinispan.Cache;
import org.jboss.as.controller.PathElement;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/invalidation-cache=*
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class InvalidationCacheResourceDefinition extends ClusteredCacheResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);
    static final PathElement pathElement(String name) {
        return PathElement.pathElement("invalidation-cache", name);
    }

    InvalidationCacheResourceDefinition(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(WILDCARD_PATH, UnaryOperator.identity(), new ClusteredCacheServiceHandler(InvalidationCacheServiceConfigurator::new), executors);
    }
}
