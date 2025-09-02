/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.deployment;

import org.wildfly.clustering.session.cache.affinity.NarySessionAffinityConfiguration;

/**
 * Mutable configuration for ranked routing.
 * @author Paul Ferraro
 */
public class MutableRankedRoutingConfiguration implements NarySessionAffinityConfiguration {
    public static final String DEFAULT_DELIMITER = ".";
    public static final int DEFAULT_MAX_MEMBERS = 3;

    private String delimiter = DEFAULT_DELIMITER;
    private int maxMembers = DEFAULT_MAX_MEMBERS;

    @Override
    public String getDelimiter() {
        return this.delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    @Override
    public int getMaxMembers() {
        return this.maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    public void setMaxMembers(String maxMembers) {
        this.setMaxMembers(Integer.valueOf(maxMembers));
    }
}
