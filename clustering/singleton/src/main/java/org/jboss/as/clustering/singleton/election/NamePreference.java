package org.jboss.as.clustering.singleton.election;

import org.jboss.as.clustering.ClusterNode;

public class NamePreference implements Preference {
    private final String name;

    public NamePreference(String name) {
        this.name = name;
    }

    @Override
    public boolean preferred(ClusterNode node) {
        return node.getName().equals(this.name);
    }
}
