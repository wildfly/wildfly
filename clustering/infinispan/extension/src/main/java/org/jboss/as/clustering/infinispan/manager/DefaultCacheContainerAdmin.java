/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.manager;

import java.util.EnumSet;

import javax.security.auth.Subject;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.manager.EmbeddedCacheManagerAdmin;

/**
 * Custom {@link EmbeddedCacheManagerAdmin} that does not use a GlobalConfigurationManager.
 * @author Paul Ferraro
 */
public class DefaultCacheContainerAdmin implements EmbeddedCacheManagerAdmin {

    private final EmbeddedCacheManager manager;

    public DefaultCacheContainerAdmin(EmbeddedCacheManager manager) {
        this.manager = manager;
    }

    @Override
    public void createTemplate(String name, Configuration configuration) {
        this.manager.defineConfiguration(name, configuration);
    }

    @Override
    public Configuration getOrCreateTemplate(String name, Configuration configuration) {
        Configuration existing = this.manager.getCacheConfiguration(name);
        return (existing != null) ? existing : this.manager.defineConfiguration(name, configuration);
    }

    @Override
    public void removeTemplate(String name) {
        this.manager.undefineConfiguration(name);
    }

    @Override
    public <K, V> Cache<K, V> createCache(String name, String template) {
        return this.createCache(name, this.manager.getCacheConfiguration(template));
    }

    @Override
    public <K, V> Cache<K, V> getOrCreateCache(String name, String template) {
        return this.getOrCreateCache(name, this.manager.getCacheConfiguration(template));
    }

    @Override
    public synchronized <K, V> Cache<K, V> createCache(String name, Configuration configuration) {
        this.createTemplate(name, configuration);
        return this.manager.getCache(name);
    }

    @Override
    public synchronized <K, V> Cache<K, V> getOrCreateCache(String name, Configuration configuration) {
        return (this.manager.getCacheConfiguration(name) != null) ? this.createCache(name, configuration) : this.manager.getCache(name);
    }

    @Override
    public void removeCache(String name) {
        this.manager.undefineConfiguration(name);
    }

    @Override
    public EmbeddedCacheManagerAdmin withFlags(AdminFlag... flags) {
        return this;
    }

    @Override
    public EmbeddedCacheManagerAdmin withFlags(EnumSet<AdminFlag> flags) {
        return this;
    }

    @Override
    public EmbeddedCacheManagerAdmin withSubject(Subject subject) {
        return this;
    }

    @Override
    public void assignAlias(String aliasName, String cacheName) {
    }

    @Override
    public void updateConfigurationAttribute(String cacheName, String attribute, String value) {
    }
}
