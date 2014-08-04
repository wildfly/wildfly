package org.jboss.as.test.clustering.cluster.registry.bean;

import java.util.Collection;

public interface RegistryRetriever {
    Collection<String> getNodes();
}
