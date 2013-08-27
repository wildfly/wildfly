package org.jboss.as.test.clustering.cluster.registry.bean;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryEntryProvider;
import org.wildfly.clustering.registry.RegistryFactory;

@Singleton
@Startup
@Local(Registry.class)
public class RegistryBean implements Registry<String, String>, Registry.Listener<String, String> {

    @Resource(lookup = "java:jboss/clustering/registry/server/default")
    private RegistryFactory<String, String> factory;
    private Registry<String, String> registry;

    @PostConstruct
    public void init() {
        RegistryEntryProvider<String, String> provider = new RegistryEntryProvider<String, String>() {
            @Override
            public String getKey() {
                return System.getProperty("jboss.node.name");
            }

            @Override
            public String getValue() {
                try {
                    return InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    return null;
                }
            }
        };
        this.registry = this.factory.createRegistry(provider);
        this.registry.addListener(this);
    }

    @PreDestroy
    public void destroy() {
        this.registry.removeListener(this);
        this.registry.close();
    }

    @Override
    public void close() {
        // We'll close on destroy
    }

    @Override
    public void addedEntries(Map<String, String> added) {
        System.out.println("New registry entry:" + added);
    }

    @Override
    public void updatedEntries(Map<String, String> updated) {
        System.out.println("Updated registry entry:" + updated);
    }

    @Override
    public void removedEntries(Map<String, String> removed) {
        System.out.println("Removed registry entry:" + removed);
    }

    @Override
    public Group getGroup() {
        return this.registry.getGroup();
    }

    @Override
    public void addListener(Registry.Listener<String, String> listener) {
        this.registry.addListener(listener);
    }

    @Override
    public void removeListener(Registry.Listener<String, String> listener) {
        this.registry.removeListener(listener);
    }

    @Override
    public Map<String, String> getEntries() {
        return this.registry.getEntries();
    }

    @Override
    public Map.Entry<String, String> getLocalEntry() {
        return this.registry.getLocalEntry();
    }

    @Override
    public Map.Entry<String, String> getEntry(Node node) {
        return this.registry.getEntry(node);
    }
}
