/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.config;

import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;
import org.jboss.as.server.deployment.AttachmentKey;

import javax.persistence.spi.PersistenceProvider;

/**
 * holds the deployed persistence provider + adaptor
 *
 * @author Scott Marlow
 */
public class PersistenceProviderDeploymentHolder {

    /**
     * List<PersistenceUnitMetadataImpl> that represents the JPA persistent units
     */
    public static final AttachmentKey<PersistenceProviderDeploymentHolder> DEPLOYED_PERSISTENCE_PROVIDER = AttachmentKey.create(PersistenceProviderDeploymentHolder.class);

    private PersistenceProvider provider;
    private PersistenceProviderAdaptor adapter;
    private String persistenceProviderAdaptorClassName;

    public PersistenceProviderAdaptor getAdapter() {
        return adapter;
    }

    public void setAdapter(PersistenceProviderAdaptor adapter) {
        this.adapter = adapter;
    }

    /**
     * returns the persistence provider that is deployed with an application.
     * returns null if no provider is deployed with the application.
     * @return the deployed persistence provider
     */
    public PersistenceProvider getProvider() {
        return provider;
    }

    public void setProvider(PersistenceProvider provider) {
        this.provider = provider;
    }

    public String getPersistenceProviderAdaptorClassName() {
        return persistenceProviderAdaptorClassName;
    }

    public void setPersistenceProviderAdaptorClassName(String persistenceProviderAdaptorClassName) {
        this.persistenceProviderAdaptorClassName = persistenceProviderAdaptorClassName;
    }

}
