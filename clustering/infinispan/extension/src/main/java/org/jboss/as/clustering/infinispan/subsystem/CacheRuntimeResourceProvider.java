/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.clustering.controller.ChildResourceProvider;
import org.jboss.as.clustering.controller.ComplexResource;
import org.jboss.as.clustering.controller.SimpleChildResourceProvider;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.wildfly.common.function.Functions;

/**
 * @author Paul Ferraro
 */
public class CacheRuntimeResourceProvider extends SimpleChildResourceProvider {

    private static final ChildResourceProvider CHILD_PROVIDER = new SimpleChildResourceProvider(Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            LockingRuntimeResourceDefinition.PATH.getValue(),
            PersistenceRuntimeResourceDefinition.PATH.getValue(),
            PartitionHandlingRuntimeResourceDefinition.PATH.getValue(),
            TransactionRuntimeResourceDefinition.PATH.getValue()))));

    public CacheRuntimeResourceProvider() {
        super(ConcurrentHashMap.newKeySet(), Functions.constantSupplier(new ComplexResource(PlaceholderResource.INSTANCE, Collections.singletonMap(CacheComponentRuntimeResourceDefinition.WILDCARD_PATH.getKey(), CHILD_PROVIDER))));
    }
}
