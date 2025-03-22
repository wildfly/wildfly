/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.deployment;

import org.wildfly.clustering.session.cache.affinity.NarySessionAffinityConfiguration;

/**
 * @author Paul Ferraro
 */
public class MutableRankedRoutingConfiguration implements NarySessionAffinityConfiguration {
    public static final String DEFAULT_DELIMITER = ".";
    public static final int DEFAULT_MAX_MEMBERS = 3;

    private String delimter = DEFAULT_DELIMITER;
    private int maxMembers = DEFAULT_MAX_MEMBERS;

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
