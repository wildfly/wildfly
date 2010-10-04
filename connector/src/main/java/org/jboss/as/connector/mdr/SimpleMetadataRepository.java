/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.connector.mdr;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.jca.common.api.metadata.ironjacamar.IronJacamar;
import org.jboss.jca.common.api.metadata.ra.Connector;
import org.jboss.jca.core.spi.mdr.AlreadyExistsException;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.mdr.NotFoundException;

/**
 * A simple implementation of the metadata repository
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
public class SimpleMetadataRepository implements MetadataRepository {

    /** Resource adapter templates */
    private ConcurrentMap<URL, Connector> raTemplates;

    /** Resource adapter roots */
    private ConcurrentMap<URL, File> raRoots;

    /** IronJacamar metadata */
    private Map<URL, IronJacamar> ironJacamar;

    /** JNDI mappings */
    private ConcurrentMap<URL, Map<String, List<String>>> jndiMappings;

    /**
     * Constructor
     */
    public SimpleMetadataRepository() {
        this.raTemplates = new ConcurrentHashMap<URL, Connector>();
        this.raRoots = new ConcurrentHashMap<URL, File>();
        this.ironJacamar = new HashMap<URL, IronJacamar>();
        this.jndiMappings = new ConcurrentHashMap<URL, Map<String, List<String>>>();
    }

    /**
     * {@inheritDoc}
     */
    public void registerResourceAdapter(URL deployment, File root, Connector md, IronJacamar ijmd)
            throws AlreadyExistsException {
        if (deployment == null)
            throw new IllegalArgumentException("Deployment is null");

        if (md == null)
            throw new IllegalArgumentException("Metadata is null");

        if (raTemplates.containsKey(deployment))
            throw new AlreadyExistsException(deployment + " already registered");

        raTemplates.put(deployment, md);
        raRoots.put(deployment, root);
        ironJacamar.put(deployment, ijmd);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterResourceAdapter(URL deployment) throws NotFoundException {
        if (deployment == null)
            throw new IllegalArgumentException("Deployment is null");

        if (!raTemplates.containsKey(deployment))
            throw new NotFoundException(deployment + " isn't registered");

        raTemplates.remove(deployment);
        raRoots.remove(deployment);
        ironJacamar.remove(deployment);
    }

    /**
     * {@inheritDoc}
     */
    public Connector getResourceAdapter(URL deployment) throws NotFoundException {
        if (deployment == null)
            throw new IllegalArgumentException("Deployment is null");

        if (!raTemplates.containsKey(deployment))
            throw new NotFoundException(deployment + " isn't registered");

        Connector md = raTemplates.get(deployment);

        // Always return a copy as the caller may make changes to it
        return (Connector) md.copy();
    }

    /**
     * {@inheritDoc}
     */
    public Set<URL> getResourceAdapters() {
        return Collections.unmodifiableSet(raTemplates.keySet());
    }

    /**
     * {@inheritDoc}
     */
    public File getRoot(URL deployment) throws NotFoundException {
        if (deployment == null)
            throw new IllegalArgumentException("Deployment is null");

        if (!raRoots.containsKey(deployment))
            throw new NotFoundException(deployment + " isn't registered");

        return raRoots.get(deployment);
    }

    /**
     * {@inheritDoc}
     */
    public IronJacamar getIronJacamar(URL deployment) throws NotFoundException {
        if (deployment == null)
            throw new IllegalArgumentException("Deployment is null");

        if (!ironJacamar.containsKey(deployment))
            throw new NotFoundException(deployment + " isn't registered");

        return ironJacamar.get(deployment);
    }

    /**
     * {@inheritDoc}
     */
    public void registerJndiMapping(URL deployment, String clz, String jndi) {
        if (deployment == null)
            throw new IllegalArgumentException("Deployment is null");

        if (clz == null)
            throw new IllegalArgumentException("Clz is null");

        if (jndi == null)
            throw new IllegalArgumentException("Jndi is null");

        Map<String, List<String>> mappings = jndiMappings.get(deployment);
        if (mappings == null) {
            Map<String, List<String>> newMappings = new HashMap<String, List<String>>(1);
            mappings = jndiMappings.putIfAbsent(deployment, newMappings);

            if (mappings == null) {
                mappings = newMappings;
            }
        }

        List<String> l = mappings.get(clz);

        if (l == null)
            l = new ArrayList<String>(1);

        l.add(jndi);
        mappings.put(clz, l);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterJndiMapping(URL deployment, String clz, String jndi) {
        if (deployment == null)
            throw new IllegalArgumentException("Deployment is null");

        if (clz == null)
            throw new IllegalArgumentException("Clz is null");

        if (jndi == null)
            throw new IllegalArgumentException("Jndi is null");

        Map<String, List<String>> mappings = jndiMappings.get(deployment);

        if (mappings != null) {
            List<String> l = mappings.get(clz);

            if (l != null) {
                l.remove(jndi);

                if (l.size() == 0) {
                    mappings.remove(clz);
                }
            }

            if (mappings.size() == 0) {
                jndiMappings.remove(deployment);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, List<String>> getJndiMappings(URL deployment) {
        if (deployment == null)
            throw new IllegalArgumentException("Deployment is null");

        return jndiMappings.get(deployment);
    }
}
