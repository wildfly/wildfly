/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.registry.bean;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.AbstractMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import org.wildfly.clustering.server.Group;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.Registration;
import org.wildfly.clustering.server.registry.Registry;
import org.wildfly.clustering.server.registry.RegistryFactory;
import org.wildfly.clustering.server.registry.RegistryListener;

@Singleton
@Startup
@Local(Registry.class)
public class RegistryBean implements Registry<GroupMember, String, String>, RegistryListener<String, String> {

    @Resource(name = "clustering/registry-factory")
    private RegistryFactory<GroupMember, String, String> factory;
    private Registry<GroupMember, String, String> registry;
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
    public void added(Map<String, String> added) {
        try {
            // Ensure the thread context classloader of the notification is correct
            Thread.currentThread().getContextClassLoader().loadClass(this.getClass().getName());
            // Ensure the correct naming context is set
            Context context = new InitialContext();
            try {
                context.lookup("java:comp/env/clustering/registry-factory");
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
    public void updated(Map<String, String> updated) {
        try {
            // Ensure the thread context classloader of the notification is correct
            Thread.currentThread().getContextClassLoader().loadClass(this.getClass().getName());
            // Ensure the correct naming context is set
            Context context = new InitialContext();
            try {
                context.lookup("java:comp/env/clustering/registry-factory");
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
    public void removed(Map<String, String> removed) {
        try {
            // Ensure the thread context classloader of the notification is correct
            Thread.currentThread().getContextClassLoader().loadClass(this.getClass().getName());
            // Ensure the correct naming context is set
            Context context = new InitialContext();
            try {
                context.lookup("java:comp/env/clustering/registry-factory");
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
    public Group<GroupMember> getGroup() {
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
    public Map.Entry<String, String> getEntry(GroupMember member) {
        return this.registry.getEntry(member);
    }
}
