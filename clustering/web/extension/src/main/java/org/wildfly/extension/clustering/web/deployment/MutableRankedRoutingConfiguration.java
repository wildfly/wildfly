/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.deployment;

import org.wildfly.clustering.session.cache.affinity.NarySessionAffinityConfiguration;
import org.wildfly.extension.clustering.web.RankedAffinityResourceDefinition;

/**
 * @author Paul Ferraro
 */
public class MutableRankedRoutingConfiguration implements NarySessionAffinityConfiguration {

    private String delimter = RankedAffinityResourceDefinition.Attribute.DELIMITER.getDefinition().getDefaultValue().asString();
    private int maxMembers = RankedAffinityResourceDefinition.Attribute.MAX_ROUTES.getDefinition().getDefaultValue().asInt();

    @Override
    public String getDelimiter() {
        return this.delimter;
    }

    public void setDelimiter(String delimiter) {
        this.delimter = delimiter;
    }

    @Override
    public int getMaxMembers() {
        return this.maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }
}
