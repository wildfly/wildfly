/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.deployment;

import org.wildfly.clustering.web.infinispan.routing.RankedRoutingConfiguration;
import org.wildfly.extension.clustering.web.RankedAffinityResourceDefinition;

/**
 * @author Paul Ferraro
 */
public class MutableRankedRoutingConfiguration implements RankedRoutingConfiguration {

    private String delimter = RankedAffinityResourceDefinition.Attribute.DELIMITER.getDefinition().getDefaultValue().asString();
    private int maxRoutes = RankedAffinityResourceDefinition.Attribute.MAX_ROUTES.getDefinition().getDefaultValue().asInt();

    @Override
    public String getDelimiter() {
        return this.delimter;
    }

    public void setDelimiter(String delimiter) {
        this.delimter = delimiter;
    }

    @Override
    public int getMaxRoutes() {
        return this.maxRoutes;
    }

    public void setMaxRoutes(int maxRoutes) {
        this.maxRoutes = maxRoutes;
    }
}
