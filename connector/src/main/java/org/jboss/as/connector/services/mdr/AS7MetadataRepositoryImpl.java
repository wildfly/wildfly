/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.mdr;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.jca.common.api.metadata.spec.Connector;
import org.jboss.jca.core.mdr.SimpleMetadataRepository;
import org.jboss.jca.core.spi.mdr.AlreadyExistsException;
import org.jboss.jca.core.spi.mdr.NotFoundException;

/**
 * An AS7' implementation of MetadataRepository
 *
 * @author Stefano Maestri (c) 2011 Red Hat Inc.
 */
public class AS7MetadataRepositoryImpl extends SimpleMetadataRepository implements AS7MetadataRepository {

    private final ConcurrentMap<String, Activation> ironJacamarMetaData = new ConcurrentHashMap<>();

    /**
     * Constructor
     */
    public AS7MetadataRepositoryImpl() {
    }

    @Override
    public synchronized void registerResourceAdapter(String uniqueId, File root, Connector md, Activation activation)
        throws AlreadyExistsException {
        super.registerResourceAdapter(uniqueId, root, md, activation);
        if (activation != null) {
            ironJacamarMetaData.put(uniqueId, activation);
        }
    }

    @Override
    public synchronized void unregisterResourceAdapter(String uniqueId) throws NotFoundException {
        super.unregisterResourceAdapter(uniqueId);
        ironJacamarMetaData.remove(uniqueId);
    }

    @Override
    public synchronized boolean hasResourceAdapter(String uniqueId) {
        return super.hasResourceAdapter(uniqueId);
    }

    @Override
    public synchronized Connector getResourceAdapter(String uniqueId) throws NotFoundException {
        return super.getResourceAdapter(uniqueId);
    }

    @Override
    public synchronized Set<String> getResourceAdapters() {
        return super.getResourceAdapters();
    }

    @Override
    public synchronized File getRoot(String uniqueId) throws NotFoundException {
        return super.getRoot(uniqueId);
    }

    @Override
    public synchronized Activation getActivation(String uniqueId) throws NotFoundException {
        return super.getActivation(uniqueId);
    }

    @Override
    public synchronized void registerJndiMapping(String uniqueId, String clz, String jndi) {
        super.registerJndiMapping(uniqueId, clz, jndi);
    }

    @Override
    public synchronized void unregisterJndiMapping(String uniqueId, String clz, String jndi) throws NotFoundException {
        super.unregisterJndiMapping(uniqueId, clz, jndi);
    }

    @Override
    public synchronized boolean hasJndiMappings(String uniqueId) {
        return super.hasJndiMappings(uniqueId);
    }

    @Override
    public synchronized Map<String, List<String>> getJndiMappings(String uniqueId) throws NotFoundException {
        return super.getJndiMappings(uniqueId);
    }

    @Override
    public synchronized Activation getIronJacamarMetaData(String uniqueId) {
        return ironJacamarMetaData.get(uniqueId);
    }

    @Override
    public synchronized Set<String> getResourceAdaptersWithIronJacamarMetadata() {
        return Collections.unmodifiableSet(ironJacamarMetaData.keySet());
    }
}
