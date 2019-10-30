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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.spi.PersistenceProvider;

import org.jboss.as.jpa.processor.JpaAttachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jipijapa.plugin.spi.PersistenceProviderAdaptor;

/**
 * holds the persistence providers + adaptors associated with a deployment
 *
 * @author Scott Marlow
 */
public class PersistenceProviderDeploymentHolder {

    private final Map<String, PersistenceProvider> providerMap = Collections.synchronizedMap(new HashMap<>());
    private final List<PersistenceProviderAdaptor>  adapterList = Collections.synchronizedList(new ArrayList<>());

    public PersistenceProviderDeploymentHolder(final List<PersistenceProvider> providerList, final List<PersistenceProviderAdaptor> adapterList) {
        synchronized (this.providerMap) {
            for(PersistenceProvider persistenceProvider : providerList){
                providerMap.put(persistenceProvider.getClass().getName(), persistenceProvider);
            }
        }
        if (adapterList != null) {
            this.adapterList.addAll(adapterList);
        }
    }

    /**
     * get the persistence providers adapters associated with an application deployment
     *
     * @return list of persistence provider adapters
     */
    public List<PersistenceProviderAdaptor> getAdapters() {
        return adapterList;
    }

    /**
     * get the persistence providers associated with an application deployment
     *
     * @return the persistence providers list
     */
    public Map<String, PersistenceProvider> getProviders() {
        return providerMap;
    }

    public static PersistenceProviderDeploymentHolder getPersistenceProviderDeploymentHolder(DeploymentUnit deploymentUnit) {
        deploymentUnit = DeploymentUtils.getTopDeploymentUnit(deploymentUnit);
        return deploymentUnit.getAttachment(JpaAttachments.DEPLOYED_PERSISTENCE_PROVIDER);
    }

    public static void savePersistenceProviderInDeploymentUnit(
            DeploymentUnit deploymentUnit, final List<PersistenceProvider> providerList, final List<PersistenceProviderAdaptor> adaptorList) {
        deploymentUnit = DeploymentUtils.getTopDeploymentUnit(deploymentUnit);
        PersistenceProviderDeploymentHolder persistenceProviderDeploymentHolder = getPersistenceProviderDeploymentHolder(deploymentUnit);
        if (persistenceProviderDeploymentHolder == null) {
            persistenceProviderDeploymentHolder = new PersistenceProviderDeploymentHolder(providerList, adaptorList);
            deploymentUnit.putAttachment(JpaAttachments.DEPLOYED_PERSISTENCE_PROVIDER, persistenceProviderDeploymentHolder);
        } else {
            synchronized (persistenceProviderDeploymentHolder.providerMap) {
                for(PersistenceProvider persistenceProvider : providerList){
                    persistenceProviderDeploymentHolder.providerMap.put(persistenceProvider.getClass().getName(), persistenceProvider);
                }
            }
            if (adaptorList != null) {
                persistenceProviderDeploymentHolder.adapterList.addAll(adaptorList);
            }
        }

    }

}
