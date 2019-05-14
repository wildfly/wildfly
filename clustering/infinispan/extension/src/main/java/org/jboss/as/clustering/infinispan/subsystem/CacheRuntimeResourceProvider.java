/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
