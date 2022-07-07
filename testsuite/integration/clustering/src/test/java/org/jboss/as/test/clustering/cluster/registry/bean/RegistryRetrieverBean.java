package org.jboss.as.test.clustering.cluster.registry.bean;

import java.util.Collection;
import java.util.TreeSet;

import jakarta.ejb.EJB;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

import org.wildfly.clustering.registry.Registry;

@Stateless
@Remote(RegistryRetriever.class)
public class RegistryRetrieverBean implements RegistryRetriever {
    @EJB
    private Registry<String, String> registry;

    @Override
    public Collection<String> getNodes() {
        return new TreeSet<>(this.registry.getEntries().keySet());
    }
}
