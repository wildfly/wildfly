/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.UnaryOperator;

import org.infinispan.Cache;
import org.jboss.as.controller.PathElement;
import org.wildfly.clustering.server.service.LocalCacheServiceConfiguratorProvider;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/local-cache=*
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class LocalCacheResourceDefinition extends CacheResourceDefinition<LocalCacheServiceConfiguratorProvider> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);
    static PathElement pathElement(String name) {
        return PathElement.pathElement("local-cache", name);
    }

    LocalCacheResourceDefinition(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(WILDCARD_PATH, UnaryOperator.identity(), new LocalCacheServiceHandler(), executors);
    }
}
