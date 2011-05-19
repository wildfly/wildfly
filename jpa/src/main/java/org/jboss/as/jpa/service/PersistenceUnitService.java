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

package org.jboss.as.jpa.service;

import org.jboss.as.jpa.config.PersistenceUnitMetadata;
import org.jboss.as.jpa.persistenceprovider.PersistenceProviderAdapterRegistry;
import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolverHolder;
import javax.sql.DataSource;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;

/**
 * Persistence Unit service that is created for each deployed persistence unit that will be referenced by the
 * persistence context/unit injector.
 * <p/>
 * The persistence unit scoped
 *
 * @author Scott Marlow
 */
public class PersistenceUnitService implements Service<PersistenceUnitService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("persistenceunit");

    private final InjectedValue<Map> properties = new InjectedValue<Map>();

    private final InjectedValue<DataSource> jtaDataSource = new InjectedValue<DataSource>();
    private final InjectedValue<DataSource> nonJtaDataSource = new InjectedValue<DataSource>();

    private EntityManagerFactory entityManagerFactory;
    private PersistenceUnitMetadata pu;

    public PersistenceUnitService(PersistenceUnitMetadata pu, ResourceRoot resourceRoot) {
        this.pu = pu;
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            PersistenceProvider provider = lookupProvider(pu.getPersistenceProviderClassName());

            pu.setJtaDataSource(jtaDataSource.getOptionalValue());
            pu.setNonJtaDataSource(nonJtaDataSource.getOptionalValue());
            this.entityManagerFactory = createContainerEntityManagerFactory(provider);

        } finally {
            pu.setTempClassloader(null);    // release the temp classloader (only needed when creating the EMF)
        }
    }

    @Override
    public void stop(StopContext context) {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
            entityManagerFactory = null;
        }
    }

    @Override
    public PersistenceUnitService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /**
     * Get the entity manager factory
     *
     * @return the entity manager factory
     */
    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    public Injector<Map> getPropertiesInjector() {
        return properties;
    }

    public Injector<DataSource> getJtaDataSourceInjector() {
        return jtaDataSource;
    }

    public Injector<DataSource> getNonJtaDataSourceInjector() {
        return nonJtaDataSource;
    }

    /**
     * Returns the Persistence Unit service name used for creation or lookup.
     * The service name contains the unique fully scoped persistence unit name
     *
     * @param pu persistence unit definition
     * @return
     */
    public static ServiceName getPUServiceName(PersistenceUnitMetadata pu) {
        return PersistenceUnitService.SERVICE_NAME.append(pu.getScopedPersistenceUnitName());
    }

    public static ServiceName getPUServiceName(String scopedPersistenceUnitName) {
        return PersistenceUnitService.SERVICE_NAME.append(scopedPersistenceUnitName);
    }

    /**
     * Look up the persistence provider
     *
     * @param providerName
     * @return
     */
    private PersistenceProvider lookupProvider(String providerName) {
        List<PersistenceProvider> providers =
            PersistenceProviderResolverHolder.getPersistenceProviderResolver().getPersistenceProviders();
        for (PersistenceProvider provider : providers) {
            if (provider.getClass().getName().equals(providerName)) {
                return provider;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (PersistenceProvider provider : providers) {
            sb.append(provider.getClass().getName()).append(", ");
        }
        throw new PersistenceException("PersistenceProvider '" + providerName + "' not found in {" + sb.toString() + "}");
    }


    /**
     * Create EE container entity manager factory
     * @param provider
     * @return EntityManagerFactory
     */
    private EntityManagerFactory createContainerEntityManagerFactory(PersistenceProvider provider) {

        PersistenceProviderAdaptor adaptor = PersistenceProviderAdapterRegistry.getPersistenceProviderAdaptor(pu.getPersistenceProviderClassName());
        adaptor.beforeCreateContainerEntityManagerFactory(pu);
        try {
            return provider.createContainerEntityManagerFactory(pu, properties.getValue());
        } finally {
            try {
                adaptor.afterCreateContainerEntityManagerFactory(pu);
            } finally {
                pu.setAnnotationIndex(null);    // close reference to Annotation Index (only needed during call to createContainerEntityManagerFactory)
                pu.setTempClassloader(null);    // close reference to temp classloader (only needed during call to createEntityManagerFactory)
            }
        }
    }

}
