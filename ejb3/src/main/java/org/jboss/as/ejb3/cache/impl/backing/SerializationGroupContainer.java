/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.cache.impl.backing;

import java.io.Serializable;
import java.util.UUID;

import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.PassivationManager;
import org.jboss.as.ejb3.cache.StatefulObjectFactory;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryFactory;
import org.jboss.as.ejb3.cache.spi.PassivatingBackingCache;
import org.jboss.as.ejb3.cache.spi.ReplicationPassivationManager;
import org.jboss.as.ejb3.cache.spi.SerializationGroup;
import org.jboss.logging.Logger;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.SerializabilityChecker;

/**
 * Functions as both a StatefulObjectFactory and PassivationManager for {@link SerializationGroup}s.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public class SerializationGroupContainer<K extends Serializable, V extends Cacheable<K>> implements BackingCacheEntryFactory<UUID, Cacheable<UUID>, SerializationGroup<K, V, UUID>>, ReplicationPassivationManager<UUID, SerializationGroup<K, V, UUID>>, StatefulObjectFactory<Cacheable<UUID>> {
    private static final Logger log = Logger.getLogger(SerializationGroupContainer.class);

    private PassivatingBackingCache<UUID, Cacheable<UUID>, SerializationGroup<K, V, UUID>> groupCache;
    private final PassivationManager<K, V> passivationManager;
    private final SerializationGroupSerializabilityChecker serializabilityChecker;

    private boolean clustered;

    public SerializationGroupContainer(PassivationManager<K, V> passivationManager) {
        this.serializabilityChecker = new SerializationGroupSerializabilityChecker();
        this.serializabilityChecker.addSerializabilityChecker(SerializabilityChecker.DEFAULT);
        this.passivationManager = passivationManager;
    }

    public boolean isClustered() {
        return clustered;
    }

    public void setClustered(boolean clustered) {
        this.clustered = clustered;
    }

    @Override
    public int getCurrentMarshallingVersion() {
        return this.passivationManager.getCurrentMarshallingVersion();
    }

    @Override
    public MarshallingConfiguration getMarshallingConfiguration(int version) {
        MarshallingConfiguration config = this.passivationManager.getMarshallingConfiguration(version);
        MarshallingConfiguration result = new MarshallingConfiguration();
        result.setBufferSize(config.getBufferSize());
        result.setClassCount(config.getClassCount());
        result.setClassExternalizerFactory(config.getClassExternalizerFactory());
        result.setClassResolver(config.getClassResolver());
        result.setClassTable(config.getClassTable());
        result.setExceptionListener(config.getExceptionListener());
        result.setExternalizerCreator(config.getExternalizerCreator());
        result.setInstanceCount(config.getInstanceCount());
        result.setObjectResolver(config.getObjectResolver());
        result.setObjectTable(config.getObjectTable());
        result.setSerializabilityChecker(this.serializabilityChecker);
        result.setSerializedCreator(config.getSerializedCreator());
        result.setStreamHeader(config.getStreamHeader());
        result.setVersion(config.getVersion());
        return result;
    }

    public void addMemberPassivationManager(PassivationManager<K, V> passivationManager) {
        SerializabilityChecker checker = passivationManager.getMarshallingConfiguration(passivationManager.getCurrentMarshallingVersion()).getSerializabilityChecker();
        if (checker != null) {
            this.serializabilityChecker.addSerializabilityChecker(checker);
        }
    }

    @Override
    public Cacheable<UUID> createInstance() {
        return null;
    }

    @Override
    public void destroyInstance(Cacheable<UUID> instance) {
    }

    @Override
    public SerializationGroup<K, V, UUID> createEntry(Cacheable<UUID> item) {
        SerializationGroupImpl<K, V> group = new SerializationGroupImpl<K, V>(this.groupCache.createIdentifier());
        group.setClustered(clustered);
        group.setGroupCache(groupCache);
        return group;
    }

    @Override
    public void destroyEntry(SerializationGroup<K, V, UUID> group) {
        // TODO: nothing?
    }

    @Override
    public void postActivate(SerializationGroup<K, V, UUID> group) {
        log.tracef("postActivate(%s)", group);
        // Restore ref to the groupCache in case it was lost during serialization
        group.setGroupCache(groupCache);
        group.postActivate();
    }

    @Override
    public void prePassivate(SerializationGroup<K, V, UUID> group) {
        log.tracef("prePassivate(%s)", group);
        group.prePassivate();
    }

    @Override
    public void postReplicate(SerializationGroup<K, V, UUID> group) {
        log.tracef("postReplicate(%s)", group);
        // Restore ref to the groupCache in case it was lost during serialization
        group.setGroupCache(groupCache);
        group.postReplicate();
    }

    @Override
    public void preReplicate(SerializationGroup<K, V, UUID> group) {
        log.tracef("preReplicate(%s)", group);
        group.preReplicate();
    }

    public PassivatingBackingCache<UUID, Cacheable<UUID>, SerializationGroup<K, V, UUID>> getGroupCache() {
        return groupCache;
    }

    public void setGroupCache(PassivatingBackingCache<UUID, Cacheable<UUID>, SerializationGroup<K, V, UUID>> groupCache) {
        this.groupCache = groupCache;
    }
}
