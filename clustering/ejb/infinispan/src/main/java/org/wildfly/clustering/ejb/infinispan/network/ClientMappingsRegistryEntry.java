/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.network;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;

import org.jboss.as.network.ClientMapping;

/**
 * Registry entry for the client mappings registry.
 * @author Paul Ferraro
 */
public class ClientMappingsRegistryEntry extends SimpleImmutableEntry<String, List<ClientMapping>> {
    private static final long serialVersionUID = 2252091408161700077L;

    public ClientMappingsRegistryEntry(String memberName, List<ClientMapping> mappings) {
        super(memberName, mappings);
    }
}