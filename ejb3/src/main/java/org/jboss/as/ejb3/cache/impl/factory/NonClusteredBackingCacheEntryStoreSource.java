/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ejb3.cache.impl.factory;

import java.io.File;
import java.io.Serializable;

import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.PassivationManager;
import org.jboss.as.ejb3.cache.impl.backing.SimpleBackingCacheEntryStore;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStore;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStoreSource;
import org.jboss.as.ejb3.cache.spi.SerializationGroup;
import org.jboss.as.ejb3.cache.spi.SerializationGroupMember;
import org.jboss.as.ejb3.cache.spi.impl.AbstractBackingCacheEntryStoreSource;
import org.jboss.as.ejb3.cache.spi.impl.FilePersistentObjectStore;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.server.services.path.AbstractPathService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * {@link BackingCacheEntryStoreSource} for a non-clustered cache. Uses a {@link FilePersistentObjectStore} store for
 * persistence.
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public class NonClusteredBackingCacheEntryStoreSource<K extends Serializable, V extends Cacheable<K>, G extends Serializable> extends AbstractBackingCacheEntryStoreSource<K, V, G> {
    /**
     * The default session store directory name ("<tt>ejb3/sessions</tt>").
     */
    public static final String DEFAULT_SESSION_DIRECTORY_NAME = "ejb3" + File.separatorChar + "sessions";

    /**
     * The default session group store directory name ("<tt>ejb3/sfsbgroups</tt>").
     */
    public static final String DEFAULT_GROUP_DIRECTORY_NAME = "ejb3" + File.separatorChar + "groups";

    public static final String DEFAULT_RELATIVE_TO = ServerEnvironment.SERVER_DATA_DIR;
    public static final int DEFAULT_SUBDIRECTORY_COUNT = 100;

    private final InjectedValue<String> relativeTo = new InjectedValue<String>();
    private final InjectedValue<ServerEnvironment> environment = new InjectedValue<ServerEnvironment>();
    private String sessionDirectoryName = DEFAULT_SESSION_DIRECTORY_NAME;
    private String groupDirectoryName = DEFAULT_GROUP_DIRECTORY_NAME;
    private String relativeToRef = DEFAULT_RELATIVE_TO;
    private int subdirectoryCount = DEFAULT_SUBDIRECTORY_COUNT;

    @Override
    public <E extends SerializationGroup<K, V, G>> BackingCacheEntryStore<G, Cacheable<G>, E> createGroupIntegratedObjectStore(PassivationManager<G, E> passivationManager, StatefulTimeoutInfo timeout) {
        FilePersistentObjectStore<G, E> objectStore = new FilePersistentObjectStore<G, E>(passivationManager.getMarshallingConfiguration(), this.getStoragePath(null, this.groupDirectoryName), subdirectoryCount);

        SimpleBackingCacheEntryStore<G, Cacheable<G>, E> store = new SimpleBackingCacheEntryStore<G, Cacheable<G>, E>(objectStore, this.environment.getValue(), timeout, this);

        return store;
    }

    @Override
    public <E extends SerializationGroupMember<K, V, G>> BackingCacheEntryStore<K, V, E> createIntegratedObjectStore(String beanName, PassivationManager<K, E> passivationManager, StatefulTimeoutInfo timeout) {
        FilePersistentObjectStore<K, E> objectStore = new FilePersistentObjectStore<K, E>(passivationManager.getMarshallingConfiguration(), this.getStoragePath(beanName, this.sessionDirectoryName), subdirectoryCount);

        SimpleBackingCacheEntryStore<K, V, E> store = new SimpleBackingCacheEntryStore<K, V, E>(objectStore, this.environment.getValue(), timeout, this);

        return store;
    }

    @Override
    public void addDependencies(ServiceTarget target, ServiceBuilder<?> builder) {
        builder.addDependency(AbstractPathService.pathNameOf(this.relativeToRef), String.class, this.relativeTo);
        builder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, this.environment);
    }

    private String getStoragePath(String beanName, String subDirectory) {
        String relativeTo = this.relativeTo.getOptionalValue();
        File path = (relativeTo != null) ? new File(new File(relativeTo), subDirectory) : new File(subDirectory);
        if (beanName != null) {
            path = new File(path, beanName);
        }
        return path.getAbsolutePath();
    }

    /**
     * Gets the name of the subdirectory under the {@link #getBaseDirectoryName() base directory} under which sessions should be
     * stored. Default is {@link #DEFAULT_SESSION_DIRECTORY_NAME}.
     */
    public String getSessionDirectoryName() {
        return sessionDirectoryName;
    }

    /**
     * Sets the name of the subdirectory under the {@link #getBaseDirectoryName() base directory} under which sessions should be
     * stored.
     */
    public void setSessionDirectoryName(String directoryName) {
        this.sessionDirectoryName = directoryName;
    }

    /**
     * Gets the name of the subdirectory under the {@link #getBaseDirectoryName() base directory} under which session groups
     * should be stored. Default is {@link #DEFAULT_GROUP_DIRECTORY_NAME}.
     */
    public String getGroupDirectoryName() {
        return groupDirectoryName;
    }

    /**
     * Sets the name of the subdirectory under the {@link #getBaseDirectoryName() base directory} under which session groups
     * should be stored.
     */
    public void setGroupDirectoryName(String groupDirectoryName) {
        this.groupDirectoryName = groupDirectoryName;
    }

    /**
     * Gets the number of subdirectories under the session directory or the group directory into which the sessions/groups
     * should be divided. Using subdirectories helps overcome filesystem limits on the number of items that can be stored.
     * Default is {@link #DEFAULT_SUBDIRECTORY_COUNT}.
     */
    public int getSubdirectoryCount() {
        return subdirectoryCount;
    }

    /**
     * Sets the number of subdirectories under the session directory or the group directory into which the sessions/groups
     * should be divided.
     */
    public void setSubdirectoryCount(int subdirectoryCount) {
        this.subdirectoryCount = subdirectoryCount;
    }

    public String getRelativeTo() {
        return this.relativeToRef;
    }

    public void setRelativeTo(String relativeTo) {
        this.relativeToRef = relativeTo;
    }
}
