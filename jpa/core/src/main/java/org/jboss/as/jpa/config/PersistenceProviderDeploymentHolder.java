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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.spi.PersistenceProvider;

import org.jipijapa.plugin.spi.PersistenceProviderAdaptor;

/**
 * holds the deployed persistence provider + adaptor
 *
 * @author Scott Marlow
 */
public class PersistenceProviderDeploymentHolder {


    private final List<PersistenceProvider> providerList = new ArrayList<PersistenceProvider>();
    private final PersistenceProviderAdaptor adapter;

    public PersistenceProviderDeploymentHolder(final List<PersistenceProvider> providerList) {
        this(providerList, null);
    }

    public PersistenceProviderDeploymentHolder(final List<PersistenceProvider> providerList, final PersistenceProviderAdaptor adapter) {
        this.providerList.addAll(providerList);
        this.adapter = adapter;
    }

    public PersistenceProviderAdaptor getAdapter() {
        return adapter;
    }

    /**
     * returns the persistence providers that are deployed with an application.
     * returns null if no provider is deployed with the application.
     *
     * @return the deployed persistence provider list
     */
    public List<PersistenceProvider> getProvider() {
        return providerList;
    }
}
