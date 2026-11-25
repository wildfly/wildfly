/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton.election;

import org.wildfly.clustering.group.Node;

/**
 * An election preference.
 * @author Paul Ferraro
 * @deprecated Replaced by {@link SingletonElectionPolicy#prefer(java.util.function.Predicate)}.
 */
@Deprecated(forRemoval = true)
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
