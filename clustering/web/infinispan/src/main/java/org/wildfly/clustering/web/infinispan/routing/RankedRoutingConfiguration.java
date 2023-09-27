/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.routing;

/**
 * @author Paul Ferraro
 */
public interface RankedRoutingConfiguration {

    String getDelimiter();

    int getMaxRoutes();
}
