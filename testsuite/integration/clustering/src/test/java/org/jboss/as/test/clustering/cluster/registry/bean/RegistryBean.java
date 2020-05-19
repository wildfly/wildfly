package org.jboss.as.test.clustering.cluster.registry.bean;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.AbstractMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.wildfly.clustering.Registration;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.registry.RegistryListener;

@Singleton
@Startup
@Local(Registry.class)
public class RegistryBean implements Registry<String, String>, RegistryListener<String, String> {

    @Resource(name = "clustering/registry")
    private RegistryFactory<String, String> factory;
    private Registry<String, String> registry;
    private Registration registration;

    private static String getLocalHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    @PostConstruct
    public void init() {
        this.registry = this.factory.createRegistry(new AbstractMap.SimpleImmutableEntry<>(System.getProperty("jboss.node.name"), getLocalHost()));
        this.registration = this.registry.register(this);
    }

    @PreDestroy
    public void destroy() {
        this.registration.close();
        this.registry.close();
    }

    @Override
    public void close() {
        // We'll close on destroy
    }

    @Override
    public void addedEntries(Map<String, String> added) {
        try {
            // Ensure the thread context classloader of the notification is correct
            Thread.currentThread().getContextClassLoader().loadClass(this.getClass().getName());
            // Ensure the correct naming context is set
            Context context = new InitialContext();
            try {
                context.lookup("java:comp/env/clustering/registry");
            } finally {
                context.close();
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (NamingException e) {
            throw new IllegalStateException(e);
        }
        System.out.println("New registry entry:" + added);
    }

    @Override
    public void updatedEntries(Map<String, String> updated) {
        try {
            // Ensure the thread context classloader of the notification is correct
            Thread.currentThread().getContextClassLoader().loadClass(this.getClass().getName());
            // Ensure the correct naming context is set
            Context context = new InitialContext();
            try {
                context.lookup("java:comp/env/clustering/registry");
            } finally {
                context.close();
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (NamingException e) {
            throw new IllegalStateException(e);
        }
        System.out.println("Updated registry entry:" + updated);
    }

    @Override
    public void removedEntries(Map<String, String> removed) {
        try {
            // Ensure the thread context classloader of the notification is correct
            Thread.currentThread().getContextClassLoader().loadClass(this.getClass().getName());
            // Ensure the correct naming context is set
            Context context = new InitialContext();
            try {
                context.lookup("java:comp/env/clustering/registry");
            } finally {
                context.close();
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (NamingException e) {
            throw new IllegalStateException(e);
        }
        System.out.println("Removed registry entry:" + removed);
    }

    @Override
    public Group getGroup() {
        return this.registry.getGroup();
    }

    @Override
    public Registration register(RegistryListener<String, String> listener) {
        return this.registry.register(listener);
    }

    @Override
    public Map<String, String> getEntries() {
        return this.registry.getEntries();
    }

    @Override
    public Map.Entry<String, String> getEntry(Node node) {
        return this.registry.getEntry(node);
    }
}
