/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton.election;

import org.wildfly.clustering.group.Node;

public class NamePreference implements Preference {
    private final String name;

    public NamePreference(String name) {
        this.name = name;
    }

    @Override
    public boolean preferred(Node node) {
        return node.getName().equals(this.name);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
