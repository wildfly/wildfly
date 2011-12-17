package org.jboss.as.clustering.singleton.election;

import org.jboss.as.clustering.ClusterNode;

public interface Preference {
    boolean preferred(ClusterNode node);
}
