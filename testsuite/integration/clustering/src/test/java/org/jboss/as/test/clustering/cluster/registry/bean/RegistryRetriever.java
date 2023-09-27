/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.registry.bean;

import java.util.Collection;

public interface RegistryRetriever {
    Collection<String> getNodes();
}
